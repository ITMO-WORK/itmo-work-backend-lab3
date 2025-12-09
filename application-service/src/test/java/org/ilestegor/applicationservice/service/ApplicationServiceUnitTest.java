package org.ilestegor.applicationservice.service;


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
import org.ilestegor.applicationservice.model.ApplicationStatus;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import org.ilestegor.applicationservice.repository.ApplicationRepository;
import org.ilestegor.applicationservice.service.interfaces.ApplicationStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ApplicationServiceUnitTest {
    @Mock
    private UserClient userClient;

    @Mock
    private VacancyClient vacancyClient;

    @Mock
    private CompanyClient companyClient;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationStatusService applicationStatusService;

    @Mock
    private ApplicationMapper applicationMapper;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private UUID vacancyId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        vacancyId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Nested
    class CreateApplicationTests {
        @Test
        void shouldCreateApplicationSuccessfully() {

            ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");

            UserResponseDto userDto = new UserResponseDto(
                    userId,
                    "John Doe",
                    "john@example.com"
            );
            when(userClient.isUserExistsById(userId)).thenReturn(userDto);


            when(vacancyClient.isVacancyExists(vacancyId)).thenReturn(true);


            when(vacancyClient.isVacancyPublished(vacancyId)).thenReturn(true);


            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(false));


            ApplicationStatus status = new ApplicationStatus(
                    1L,
                    ApplicationStatusName.NEW
            );

            when(applicationStatusService.findApplicationStatusByApplicationStatusName(ApplicationStatusName.NEW))
                    .thenReturn(Mono.just(status));


            var savedApp = Application.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .vacancyId(vacancyId)
                    .coverLetter(request.coverLetter())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .status(status.getId())
                    .build();

            when(applicationRepository.save(any(Application.class)))
                    .thenReturn(Mono.just(savedApp));


            Mono<ApplicationCreateResponseDto> result =
                    applicationService.createApplication(vacancyId, userId, request);


            StepVerifier.create(result)
                    .assertNext(dto -> {
                        assertEquals(savedApp.getId(), dto.id());
                        assertEquals(ApplicationStatusName.NEW.getValue(), dto.status());
                        assertEquals(savedApp.getCoverLetter(), dto.coverLetter());
                    })
                    .verifyComplete();
        }

        @Test
        void shouldFailWhenUserNotFound() {

            ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");


            UserResponseDto mock = new UserResponseDto(
                    null,
                    "Test",
                    "t@mail.com"
            );
            when(userClient.isUserExistsById(userId)).thenReturn(mock);


            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(false));


            when(applicationStatusService.findApplicationStatusByApplicationStatusName(ApplicationStatusName.NEW))
                    .thenReturn(Mono.empty());


            Mono<ApplicationCreateResponseDto> result =
                    applicationService.createApplication(vacancyId, userId, request);


            StepVerifier.create(result)
                    .expectError(UserNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenVacancyNotFound() {

            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");


            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));


            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(false);


            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(false));

            when(applicationStatusService
                    .findApplicationStatusByApplicationStatusName(ApplicationStatusName.NEW))
                    .thenReturn(Mono.just(new ApplicationStatus(
                            1L,
                            ApplicationStatusName.NEW
                    )));


            Mono<ApplicationCreateResponseDto> result =
                    applicationService.createApplication(vacancyId, userId, request);


            StepVerifier.create(result)
                    .expectError(VacancyNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenVacancyNotPublished() {

            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");


            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            
            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(false);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(false));

            when(applicationStatusService
                    .findApplicationStatusByApplicationStatusName(ApplicationStatusName.NEW))
                    .thenReturn(Mono.just(new ApplicationStatus(
                            1L,
                            ApplicationStatusName.NEW
                    )));

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.createApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(VacancyNotPublishedException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenUserAlreadyApplied() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(true);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            
            when(applicationStatusService
                    .findApplicationStatusByApplicationStatusName(ApplicationStatusName.NEW))
                    .thenReturn(Mono.just(new ApplicationStatus(
                            1L,
                            ApplicationStatusName.NEW
                    )));

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.createApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserHasAlreadyAppliedException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenApplicationStatusNotFound() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(true);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(false));

            
            when(applicationStatusService
                    .findApplicationStatusByApplicationStatusName(ApplicationStatusName.NEW))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.createApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(ApplicationStatusNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    class UpdateApplicationTests {
        @Test
        void shouldUpdateApplicationSuccessfully() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("updated cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "John Doe",
                            "john@example.com"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            
            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(true);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            
            Application existing = Application.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .vacancyId(vacancyId)
                    .coverLetter("old cover")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .status(1L)
                    .build();

            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(existing));

            
            doAnswer(invocation -> {
                Application app = invocation.getArgument(0, Application.class);
                ApplicationCreateRequestDto dto = invocation.getArgument(1, ApplicationCreateRequestDto.class);
                app.setCoverLetter(dto.coverLetter());
                return null;
            }).when(applicationMapper).update(any(Application.class), any(ApplicationCreateRequestDto.class));

            
            Application saved = Application.builder()
                    .id(existing.getId())
                    .userId(userId)
                    .vacancyId(vacancyId)
                    .coverLetter(request.coverLetter())
                    .createdAt(existing.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .status(1L)
                    .build();

            when(applicationRepository.save(any(Application.class)))
                    .thenReturn(Mono.just(saved));

            
            ApplicationStatus status = new ApplicationStatus(
                    1L,
                    ApplicationStatusName.NEW
            );

            when(applicationStatusService.findApplicationStatusByApplicationStatusId(saved.getStatus()))
                    .thenReturn(Mono.just(status));

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .assertNext(dto -> {
                        assertEquals(saved.getId(), dto.id());
                        assertEquals(ApplicationStatusName.NEW.getValue(), dto.status());
                        assertEquals(saved.getCoverLetter(), dto.coverLetter());
                    })
                    .verifyComplete();
        }

        @Test
        void shouldFailWhenUserNotFound() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            null,
                            "Test",
                            "t@mail.com"
                    ));


            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(Application.builder().build()));

            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserNotFoundException.class)
                    .verify();
        }


        @Test
        void shouldFailWhenVacancyNotFound() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(false);

            


            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(Application.builder().build()));

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(VacancyNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenVacancyNotPublished() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            
            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(false);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(Application.builder().build()));

            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(VacancyNotPublishedException.class)
                    .verify();
        }


        @Test
        void shouldFailWhenUserHasNotAppliedYet() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(true);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(false));

            
            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserApplicationNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenApplicationNotFoundForUpdate() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(true);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            
            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserApplicationNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenApplicationStatusNotFoundOnUpdate() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(true);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            
            Application existing = Application.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .vacancyId(vacancyId)
                    .coverLetter("old")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .status(1L)
                    .build();

            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(existing));

            doNothing().when(applicationMapper).update(any(Application.class), any(ApplicationCreateRequestDto.class));

            when(applicationRepository.save(any(Application.class)))
                    .thenReturn(Mono.just(existing));

            
            when(applicationStatusService.findApplicationStatusByApplicationStatusId(existing.getStatus()))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(ApplicationStatusNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenApplicationStatusIsNotNewOnUpdate() {
            
            ApplicationCreateRequestDto request =
                    new ApplicationCreateRequestDto("cover letter");

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.isVacancyPublished(vacancyId))
                    .thenReturn(true);

            
            when(applicationRepository.existsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(true));

            
            Application existing = Application.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .vacancyId(vacancyId)
                    .coverLetter("old")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .status(2L)
                    .build();

            when(applicationRepository.findApplicationsByUserIdAndVacancyId(userId, vacancyId))
                    .thenReturn(Mono.just(existing));

            doNothing().when(applicationMapper).update(any(Application.class), any(ApplicationCreateRequestDto.class));

            when(applicationRepository.save(any(Application.class)))
                    .thenReturn(Mono.just(existing));

            
            ApplicationStatus status = new ApplicationStatus(
                    2L,
                    ApplicationStatusName.VIEWED 
            );
            when(applicationStatusService.findApplicationStatusByApplicationStatusId(existing.getStatus()))
                    .thenReturn(Mono.just(status));

            
            Mono<ApplicationCreateResponseDto> result =
                    applicationService.updateApplication(vacancyId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(InvalidApplicationStatusForApplicationUpdate.class)
                    .verify();
        }

    }

    @Nested
    class UpdateApplicationStatusTests {
        private UUID applicationId;
        private UUID companyId;

        @BeforeEach
        void initStatusTests() {
            applicationId = UUID.randomUUID();
            companyId = UUID.randomUUID();
        }

        @Test
        void shouldUpdateApplicationStatusSuccessfully() {
            
            ApplicationStatusUpdateRequestDto request =
                    new ApplicationStatusUpdateRequestDto(ApplicationStatusName.NEW);

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "John Doe",
                            "john@example.com"
                    ));

            
            when(applicationRepository.findVacancyIdById(applicationId))
                    .thenReturn(Mono.just(vacancyId));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            
            when(vacancyClient.getCompanyIdByVacancy(vacancyId))
                    .thenReturn(companyId);

            when(companyClient.isUserBelongsToCompany(companyId, userId))
                    .thenReturn(true);

            
            Application application = Application.builder()
                    .id(applicationId)
                    .userId(userId)
                    .vacancyId(vacancyId)
                    .coverLetter("some")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .status(1L)
                    .build();

            when(applicationRepository.findById(applicationId))
                    .thenReturn(Mono.just(application));

            
            ApplicationStatus newStatus = new ApplicationStatus(
                    2L,
                    ApplicationStatusName.NEW
            );

            when(applicationStatusService
                    .findApplicationStatusByApplicationStatusName(request.applicationStatusName()))
                    .thenReturn(Mono.just(newStatus));

            
            Application saved = Application.builder()
                    .id(application.getId())
                    .userId(application.getUserId())
                    .vacancyId(application.getVacancyId())
                    .coverLetter(application.getCoverLetter())
                    .createdAt(application.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .status(newStatus.getId())
                    .build();

            when(applicationRepository.save(any(Application.class)))
                    .thenReturn(Mono.just(saved));

            
            Mono<ApplicationStatusUpdateResponseDto> result =
                    applicationService.updateApplicationStatus(applicationId, userId, request);

            
            StepVerifier.create(result)
                    .assertNext(dto -> {
                        assertEquals(newStatus.getApplicationStatusName().getValue(), dto.status());
                        assertEquals(saved.getUpdatedAt(), dto.updateAt());
                    })
                    .verifyComplete();
        }

        @Test
        void shouldFailWhenUserNotFound() {
            
            ApplicationStatusUpdateRequestDto request =
                    new ApplicationStatusUpdateRequestDto(ApplicationStatusName.NEW);

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            null,
                            "Test",
                            "t@mail.com"
                    ));

            
            when(applicationRepository.findVacancyIdById(applicationId))
                    .thenReturn(Mono.just(vacancyId));

            
            Mono<ApplicationStatusUpdateResponseDto> result =
                    applicationService.updateApplicationStatus(applicationId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenVacancyIdNotFoundByApplication() {
            
            ApplicationStatusUpdateRequestDto request =
                    new ApplicationStatusUpdateRequestDto(ApplicationStatusName.NEW);

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            
            when(applicationRepository.findVacancyIdById(applicationId))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationStatusUpdateResponseDto> result =
                    applicationService.updateApplicationStatus(applicationId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserApplicationNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenVacancyNotFound() {
            
            ApplicationStatusUpdateRequestDto request =
                    new ApplicationStatusUpdateRequestDto(ApplicationStatusName.NEW);

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            when(applicationRepository.findVacancyIdById(applicationId))
                    .thenReturn(Mono.just(vacancyId));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(false);


            when(applicationRepository.findById(applicationId))
                    .thenReturn(Mono.just(Application.builder().build()));


            
            Mono<ApplicationStatusUpdateResponseDto> result =
                    applicationService.updateApplicationStatus(applicationId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(VacancyNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenUserDoesNotBelongToCompany() {
            
            ApplicationStatusUpdateRequestDto request =
                    new ApplicationStatusUpdateRequestDto(ApplicationStatusName.NEW);

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            when(applicationRepository.findVacancyIdById(applicationId))
                    .thenReturn(Mono.just(vacancyId));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            
            when(vacancyClient.getCompanyIdByVacancy(vacancyId))
                    .thenReturn(companyId);

            
            when(companyClient.isUserBelongsToCompany(companyId, userId))
                    .thenReturn(false);

            
            when(applicationRepository.findById(applicationId))
                    .thenReturn(Mono.just(Application.builder().build()));


            
            Mono<ApplicationStatusUpdateResponseDto> result =
                    applicationService.updateApplicationStatus(applicationId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserDoesNotBelongsToCompanyException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenApplicationNotFoundById() {
            
            ApplicationStatusUpdateRequestDto request =
                    new ApplicationStatusUpdateRequestDto(ApplicationStatusName.NEW);

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            when(applicationRepository.findVacancyIdById(applicationId))
                    .thenReturn(Mono.just(vacancyId));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.getCompanyIdByVacancy(vacancyId))
                    .thenReturn(companyId);

            when(companyClient.isUserBelongsToCompany(companyId, userId))
                    .thenReturn(true);

            
            when(applicationRepository.findById(applicationId))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationStatusUpdateResponseDto> result =
                    applicationService.updateApplicationStatus(applicationId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(UserApplicationNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenTargetStatusNotFound() {
            
            ApplicationStatusUpdateRequestDto request =
                    new ApplicationStatusUpdateRequestDto(ApplicationStatusName.NEW);

            
            when(userClient.isUserExistsById(userId))
                    .thenReturn(new UserResponseDto(
                            userId,
                            "n",
                            "e"
                    ));

            when(applicationRepository.findVacancyIdById(applicationId))
                    .thenReturn(Mono.just(vacancyId));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.getCompanyIdByVacancy(vacancyId))
                    .thenReturn(companyId);

            when(companyClient.isUserBelongsToCompany(companyId, userId))
                    .thenReturn(true);

            
            Application application = Application.builder()
                    .id(applicationId)
                    .userId(userId)
                    .vacancyId(vacancyId)
                    .coverLetter("x")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .status(1L)
                    .build();

            when(applicationRepository.findById(applicationId))
                    .thenReturn(Mono.just(application));

            
            when(applicationStatusService
                    .findApplicationStatusByApplicationStatusName(request.applicationStatusName()))
                    .thenReturn(Mono.empty());

            
            Mono<ApplicationStatusUpdateResponseDto> result =
                    applicationService.updateApplicationStatus(applicationId, userId, request);

            
            StepVerifier.create(result)
                    .expectError(ApplicationStatusNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    class GetAllApplicationsByVacancyIdTests {
        private UUID vacancyId;
        private UUID hrUserId;      
        private UUID applicantId;   
        private UUID companyId;
        private Pageable pageable;

        @BeforeEach
        void init() {
            vacancyId = UUID.randomUUID();
            hrUserId = userId;                  
            applicantId = UUID.randomUUID();
            companyId = UUID.randomUUID();
            pageable = PageRequest.of(0, 10);
        }

        @Test
        void shouldReturnPageOfApplicationsSuccessfully() {
            
            String vacancyTitle = "Java Developer";

            
            when(userClient.isUserExistsById(hrUserId))
                    .thenReturn(new UserResponseDto(
                            hrUserId,
                            "HR User",
                            "hr@mail.com"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            
            when(vacancyClient.getCompanyIdByVacancy(vacancyId))
                    .thenReturn(companyId);

            when(companyClient.isUserBelongsToCompany(companyId, hrUserId))
                    .thenReturn(true);

            
            when(vacancyClient.getVacancyTitle(vacancyId))
                    .thenReturn(vacancyTitle);

            
            Application app1 = Application.builder()
                    .id(UUID.randomUUID())
                    .userId(applicantId)
                    .vacancyId(vacancyId)
                    .coverLetter("cover 1")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .status(1L)
                    .build();

            Application app2 = Application.builder()
                    .id(UUID.randomUUID())
                    .userId(applicantId)
                    .vacancyId(vacancyId)
                    .coverLetter("cover 2")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .updatedAt(LocalDateTime.now().minusDays(2))
                    .status(1L)
                    .build();

            when(applicationRepository.findAllByVacancyId(vacancyId, pageable))
                    .thenReturn(Flux.just(app1, app2));

            
            when(userClient.isUserExistsById(applicantId))
                    .thenReturn(new UserResponseDto(
                            applicantId,
                            "Applicant Name",
                            "applicant@mail.com"
                    ));

            
            
            when(applicationMapper.fromApplicationtoApplicationDto(any(Application.class)))
                    .thenAnswer(invocation -> {
                        Application app = invocation.getArgument(0, Application.class);
                        return ApplicationDto.builder()
                                .id(app.getId())
                                .userId(app.getUserId())
                                .vacancyId(app.getVacancyId())
                                .coverLetter(app.getCoverLetter())
                                .build();
                    });

            
            when(applicationRepository.countApplicationByVacancyId(vacancyId))
                    .thenReturn(Mono.just(2L));

            
            Mono<Page<ApplicationDto>> result =
                    applicationService.getAllApplicationsByVacancyId(vacancyId, hrUserId, pageable);

            
            StepVerifier.create(result)
                    .assertNext(page -> {
                        assertEquals(2, page.getTotalElements());
                        assertEquals(2, page.getContent().size());

                        ApplicationDto first = page.getContent().getFirst();
                        assertEquals("Applicant Name", first.userFullName());
                        assertEquals(vacancyTitle, first.vacancyTitle());
                    })
                    .verifyComplete();
        }

        @Test
        void shouldFailWhenUserNotFoundAtStart() {
            
            when(userClient.isUserExistsById(hrUserId))
                    .thenReturn(new UserResponseDto(
                            null,
                            "HR",
                            "hr@mail.com"
                    ));



            
            Mono<Page<ApplicationDto>> result =
                    applicationService.getAllApplicationsByVacancyId(vacancyId, hrUserId, pageable);

            
            StepVerifier.create(result)
                    .expectError(UserNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenVacancyNotFound() {
            
            when(userClient.isUserExistsById(hrUserId))
                    .thenReturn(new UserResponseDto(
                            hrUserId,
                            "HR",
                            "hr@mail.com"
                    ));

            
            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(false);

            


            
            Mono<Page<ApplicationDto>> result =
                    applicationService.getAllApplicationsByVacancyId(vacancyId, hrUserId, pageable);

            
            StepVerifier.create(result)
                    .expectError(VacancyNotFoundException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenUserDoesNotBelongToCompany() {
            
            when(userClient.isUserExistsById(hrUserId))
                    .thenReturn(new UserResponseDto(
                            hrUserId,
                            "HR",
                            "hr@mail.com"
                    ));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.getCompanyIdByVacancy(vacancyId))
                    .thenReturn(companyId);

            
            when(companyClient.isUserBelongsToCompany(companyId, hrUserId))
                    .thenReturn(false);

            









            
            Mono<Page<ApplicationDto>> result =
                    applicationService.getAllApplicationsByVacancyId(vacancyId, hrUserId, pageable);

            
            StepVerifier.create(result)
                    .expectError(UserDoesNotBelongsToCompanyException.class)
                    .verify();
        }

        @Test
        void shouldFailWhenInnerUserOfApplicationNotFound() {
            
            String vacancyTitle = "Java Developer";

            
            when(userClient.isUserExistsById(hrUserId))
                    .thenReturn(new UserResponseDto(
                            hrUserId,
                            "HR User",
                            "hr@mail.com"
                    ));

            when(vacancyClient.isVacancyExists(vacancyId))
                    .thenReturn(true);

            when(vacancyClient.getCompanyIdByVacancy(vacancyId))
                    .thenReturn(companyId);

            when(companyClient.isUserBelongsToCompany(companyId, hrUserId))
                    .thenReturn(true);

            when(vacancyClient.getVacancyTitle(vacancyId))
                    .thenReturn(vacancyTitle);

            
            Application app = Application.builder()
                    .id(UUID.randomUUID())
                    .userId(applicantId)
                    .vacancyId(vacancyId)
                    .coverLetter("cover")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .status(1L)
                    .build();

            when(applicationRepository.findAllByVacancyId(vacancyId, pageable))
                    .thenReturn(Flux.just(app));

            
            when(userClient.isUserExistsById(applicantId))
                    .thenReturn(new UserResponseDto(
                            null,
                            "Ghost",
                            "ghost@mail.com"
                    ));



            when(applicationRepository.countApplicationByVacancyId(vacancyId))
                    .thenReturn(Mono.just(1L));

            
            Mono<Page<ApplicationDto>> result =
                    applicationService.getAllApplicationsByVacancyId(vacancyId, hrUserId, pageable);

            
            StepVerifier.create(result)
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }

}