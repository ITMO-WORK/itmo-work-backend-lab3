package org.ilestegor.applicationservice.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.ilestegor.applicationservice.dto.ApplicationDto;
import org.ilestegor.applicationservice.dto.request.ApplicationCreateRequestDto;
import org.ilestegor.applicationservice.dto.request.ApplicationStatusUpdateRequestDto;
import org.ilestegor.applicationservice.dto.response.ApplicationCreateResponseDto;
import org.ilestegor.applicationservice.dto.response.ApplicationStatusUpdateResponseDto;
import org.ilestegor.applicationservice.exception.exceptions.*;
import org.ilestegor.applicationservice.infrastructure.feign.company.CompanyClient;
import org.ilestegor.applicationservice.infrastructure.feign.user.UserClient;
import org.ilestegor.applicationservice.infrastructure.feign.user.dto.UserResponseDto;
import org.ilestegor.applicationservice.infrastructure.feign.vacancy.VacancyClient;
import org.ilestegor.applicationservice.mapper.ApplicationMapper;
import org.ilestegor.applicationservice.model.Application;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import org.ilestegor.applicationservice.repository.ApplicationRepository;
import org.ilestegor.applicationservice.service.interfaces.ApplicationService;
import org.ilestegor.applicationservice.service.interfaces.ApplicationStatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final UserClient userClient;

    private final ApplicationMapper applicationMapper;

    private final ApplicationStatusService applicationStatusService;

    private final CompanyClient companyClient;

    private final VacancyClient vacancyClient;

    @Override
    public Mono<ApplicationCreateResponseDto> createApplication(UUID id, UUID userId, ApplicationCreateRequestDto applicationCreateRequestDto) {
        return checkUserExists(userId).then(checkVacancyExists(id)).then(checkVacancyIsPublished(id)).then(checkUserHasNotApplied(userId, id)).then(createAndSaveApplication(userId, id, applicationCreateRequestDto));
    }

    @Override
    public Mono<ApplicationCreateResponseDto> updateApplication(UUID id, UUID userId, ApplicationCreateRequestDto applicationCreateRequestDto) {
        return checkUserExists(userId).then(checkVacancyExists(id)).then(checkVacancyIsPublished(id)).then(checkUserHasAlreadyAppliedForVacancy(userId, id)).then(updateAndSaveApplication(id, userId, applicationCreateRequestDto));
    }

    //Only for admins
    @Override
    public Mono<ApplicationStatusUpdateResponseDto> updateApplicationStatus(UUID applicationId, UUID userId, ApplicationStatusUpdateRequestDto applicationStatusUpdateRequestDto) {
        return checkUserExists(userId)
                .then(getVacancyIdByApplicationId(applicationId))
                .flatMap(vacancyId ->
                        checkVacancyExists(vacancyId)
                                .then(checkUserBelongsToCompany(vacancyId, userId))
                                .then(applicationRepository.findById(applicationId)
                                        .switchIfEmpty(Mono.error(new UserApplicationNotFoundException()))
                                        .flatMap(application ->
                                                applicationStatusService.findApplicationStatusByApplicationStatusName(applicationStatusUpdateRequestDto.applicationStatusName())
                                                        .switchIfEmpty(Mono.error(new ApplicationStatusNotFoundException()))
                                                        .flatMap(status -> {
                                                            application.setStatus(status.getId());
                                                            application.setUpdatedAt(LocalDateTime.now());
                                                            return applicationRepository.save(application)
                                                                    .map(saved -> new ApplicationStatusUpdateResponseDto(
                                                                            status.getApplicationStatusName().getValue(),
                                                                            saved.getUpdatedAt()
                                                                    ));
                                                        })
                                        )
                                )
                );
    }

    @Override
    public Mono<Page<ApplicationDto>> getAllApplicationsByVacancyId(UUID vacancyId, UUID userId, Pageable pageable) {

        return checkUserExists(userId)
                .then(checkVacancyExists(vacancyId))
                .then(checkUserBelongsToCompany(vacancyId, userId))
                .then(getVacancyTitle(vacancyId))
                .flatMap(vacancyTitle -> applicationRepository.findAllByVacancyId(vacancyId, pageable)
                        .flatMap(application -> checkUserExists(application.getUserId())
                        .map(user -> {
                            ApplicationDto applicationDto = applicationMapper.fromApplicationtoApplicationDto(application);
                            return applicationDto.toBuilder().userFullName(user.fullName()).vacancyTitle(vacancyTitle)
                                    .build();
                        })
                ).collectList()
                                .zipWith(applicationRepository.countApplicationByVacancyId(vacancyId))
                                .map(application -> new PageImpl<>(application.getT1(), pageable, application.getT2())));
    }

    private Mono<UUID> getCompanyIdByVacancyId(UUID vacancyId){
        return Mono.fromCallable(() -> vacancyClient.getCompanyIdByVacancy(vacancyId))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.error(new VacancyNotFoundException()));

    }

    private Mono<Void> checkUserBelongsToCompany(UUID vacancyId,  UUID userId) {
        return getCompanyIdByVacancyId(vacancyId)
                .flatMap(companyId -> Mono.fromCallable(() -> companyClient.isUserBelongsToCompany(companyId, userId)).subscribeOn(Schedulers.boundedElastic())
                        .flatMap(belongs -> {
                            if (Boolean.TRUE.equals(belongs))
                                    return Mono.empty();
                            return Mono.error(new UserDoesNotBelongsToCompanyException());
                        }));
    }

    private Mono<String> getVacancyTitle(UUID vacancyId){
        return Mono.fromCallable(() -> vacancyClient.getVacancyTitle(vacancyId))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.error(new VacancyNotFoundException()));
    }

    private Mono<Boolean> checkVacancyIsPublished(UUID vacancyId){
        return Mono.fromCallable(() -> vacancyClient.isVacancyPublished(vacancyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.just(true);
                    }
                    return Mono.error(new VacancyNotPublishedException());
                });
    }

    private Mono<ApplicationCreateResponseDto> updateAndSaveApplication(UUID vacancyId, UUID userId, ApplicationCreateRequestDto applicationCreateRequestDto){
        return applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId).switchIfEmpty(Mono.error(new UserApplicationNotFoundException()))
                .flatMap(application -> {
                    applicationMapper.update(application, applicationCreateRequestDto);
                    application.setUpdatedAt(LocalDateTime.now());
                    return applicationRepository.save(application);
                }).flatMap(savedApplication ->
                    applicationStatusService.findApplicationStatusByApplicationStatusId(savedApplication.getStatus())
                            .switchIfEmpty(Mono.error(new ApplicationStatusNotFoundException()))
                            .flatMap(status -> {
                                if (!status.getApplicationStatusName().equals(ApplicationStatusName.NEW))
                                    return Mono.error(new InvalidApplicationStatusForApplicationUpdate());
                                var res =  new ApplicationCreateResponseDto(savedApplication.getId(), status.getApplicationStatusName().getValue(), savedApplication.getCreatedAt(), savedApplication.getUpdatedAt(), savedApplication.getCoverLetter());
                                return Mono.just(res);
                            })
                );
    }

    private Mono<Void> checkVacancyExists(UUID vacancyId){
        return Mono.fromCallable(() -> vacancyClient.isVacancyExists(vacancyId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists))
                        return Mono.empty();
                    return Mono.error(new VacancyNotFoundException());
                });
    }

    private Mono<UUID> getVacancyIdByApplicationId(UUID applicationId){
        return applicationRepository.findVacancyIdById(applicationId).switchIfEmpty(Mono.error(new UserApplicationNotFoundException()));
    }

    private Mono<Boolean> checkApplicationExists(UUID applicationId){
        return applicationRepository.existsById(applicationId).flatMap(exists -> {
            if (Boolean.TRUE.equals(exists))
                return Mono.empty();
            return Mono.error(new UserApplicationNotFoundException());
        });
    }

    private Mono<Boolean> checkUserHasAlreadyAppliedForVacancy(UUID userId, UUID vacancyId){
        return applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId).flatMap(exists ->
        {
            if (Boolean.TRUE.equals(exists)) {
                return Mono.just(true);
            }
            return Mono.error(new UserApplicationNotFoundException());
        });
    }


    private Mono<UserResponseDto> checkUserExists(UUID userId) {
        return Mono.fromCallable(() -> userClient.isUserExistsById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(FeignException.NotFound.class, ex -> new UserNotFoundException())
                .flatMap(dto -> {
                    if (dto == null || dto.id() == null) {
                        return Mono.error(new UserNotFoundException());
                    }
                    return Mono.just(dto);
                });
    }



    private Mono<Void> checkUserHasNotApplied(UUID userId, UUID vacancyId){
        return applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId).flatMap(
                exists -> {
                    if (exists) return Mono.error(new UserHasAlreadyAppliedException());
                    return Mono.empty();
                }
        );
    }

    private Mono<ApplicationCreateResponseDto> createAndSaveApplication(UUID userId, UUID vacancyId, ApplicationCreateRequestDto applicationCreateRequestDto){
        return applicationStatusService.findApplicationStatusByApplicationStatusName(ApplicationStatusName.NEW)
                .switchIfEmpty(Mono.error(new ApplicationStatusNotFoundException()))
                .flatMap(status -> {
                    Application application = Application.builder().coverLetter(applicationCreateRequestDto.coverLetter())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .status(status.getId())
                            .userId(userId)
                            .vacancyId(vacancyId).build();

                    return applicationRepository.save(application).map(saved -> new ApplicationCreateResponseDto(
                            saved.getId(),
                            status.getApplicationStatusName().getValue(),
                            saved.getCreatedAt(),
                            saved.getUpdatedAt(),
                            saved.getCoverLetter()
                    ));
                });
    }
}
