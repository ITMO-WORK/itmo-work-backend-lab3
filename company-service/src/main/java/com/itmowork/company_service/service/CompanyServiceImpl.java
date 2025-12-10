package com.itmowork.company_service.service;

import com.itmowork.company_service.client.UserClient;
import com.itmowork.company_service.configuration.UserPrincipal;
import com.itmowork.company_service.dto.request.CompanyRequestDto;
import com.itmowork.company_service.dto.request.CompanyUpdateRequestDto;
import com.itmowork.company_service.dto.request.UserRequestDto;
import com.itmowork.company_service.dto.response.CompanyDeleteResponseDto;
import com.itmowork.company_service.dto.response.CompanyResponseDto;
import com.itmowork.company_service.dto.response.UserResponseDto;
import com.itmowork.company_service.exception.exceptions.CompanyAlreadyExistsException;
import com.itmowork.company_service.exception.exceptions.CompanyNotFoundException;
import com.itmowork.company_service.exception.exceptions.UserClientException;
import com.itmowork.company_service.model.Company;
import com.itmowork.company_service.model.CompanyStatus;
import com.itmowork.company_service.model.CompanyStatusName;
import com.itmowork.company_service.model.UserCompany;
import com.itmowork.company_service.repository.CompanyRepository;
import com.itmowork.company_service.service.interfaces.CompanyService;
import com.itmowork.company_service.service.interfaces.CompanyStatusService;
import com.itmowork.company_service.service.interfaces.UserCompanyService;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;


import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    private final CompanyStatusService companyStatusService;

    private final UserClient userClient;

    private final UserCompanyService userCompanyService;

    private final CircuitBreakerRegistry registry;



    @Override
    @Transactional
    public Mono<CompanyResponseDto> createCompany(CompanyRequestDto companyRequestDto) {
        return companyRepository.existsByEmail(companyRequestDto.email())
                .flatMap(isExists -> {
                            if(isExists){
                                return Mono.error(new CompanyAlreadyExistsException("Компания с таким email уже существует"));
                            }
                            Mono<CompanyStatus> companyStatusMono = companyStatusService.
                                    findCompanyStatusByCompanyStatusName(CompanyStatusName.PENDING_VERIFICATION);


                            Mono<UserResponseDto> userResponseDtoMono = createRemoteUser(
                                    new UserRequestDto(
                                            companyRequestDto.ownerFullName(),
                                            companyRequestDto.ownerPassword(),
                                            companyRequestDto.ownerEmail()
                                    )
                            );

                            return Mono.zip(companyStatusMono, userResponseDtoMono);
                        }
                )
                .flatMap(tuple -> {
                    CompanyStatus companyStatus = tuple.getT1();
                    UserResponseDto userResponseDto = tuple.getT2();

                    Company company = getCompany(companyRequestDto, companyStatus);

                    return companyRepository.save(company)
                            .map(savedCompany -> Tuples.of(savedCompany, userResponseDto));
                })
                .flatMap(tuple -> {
                    Company savedCompany = tuple.getT1();
                    UserResponseDto userResponseDto = tuple.getT2();

                   UserCompany userCompany = new UserCompany();
                   userCompany.setCompanyId(savedCompany.getId());
                   userCompany.setUserId(userResponseDto.id());

                   return userCompanyService.saveUserCompany(userCompany);

                })
                .map(userCompany -> mapToCompanyResponseDto(companyRequestDto, userCompany));
    }

    @Override
    @Transactional
    public Mono<CompanyResponseDto> updateCompany(UUID id, CompanyUpdateRequestDto companyUpdateRequestDto) {
        return getUserId()
                .flatMap(userId ->
                        userCompanyService.validateCompanyOwnership(id, userId)
                                .filter(Boolean::booleanValue)
                                .switchIfEmpty(Mono.error(
                                        new CompanyNotFoundException("Компания не принадлежит пользователю")
                                ))
                                .flatMap(valid -> companyRepository.findCompanyById(id))
                                .switchIfEmpty(Mono.error(
                                        new CompanyNotFoundException("Компания не найдена")
                                ))
                                .flatMap(company -> {

                                    if (companyUpdateRequestDto.name() != null) company.setName(companyUpdateRequestDto.name());
                                    if (companyUpdateRequestDto.email() != null) company.setEmail(companyUpdateRequestDto.email());
                                    if (companyUpdateRequestDto.description() != null) company.setDescription(companyUpdateRequestDto.description());

                                    return companyRepository.save(company);
                                })
                                .map(saved -> CompanyResponseDto.builder()
                                        .id(saved.getId())
                                        .name(saved.getName())
                                        .email(saved.getEmail())
                                        .description(saved.getDescription())
                                        .statusMessage("Компания была успешно обновлена")
                                        .userId(userId)
                                        .build()
                                )
                );
    }

    @Override
    public Mono<CompanyDeleteResponseDto> deleteCompany(UUID id) {
        return getUserId()
                .flatMap(userId -> userCompanyService.validateCompanyOwnership(id, userId))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(
                        Mono.error(new CompanyNotFoundException(
                                "Компания с таким пользователем не найдена"
                        )))
                .flatMap(valid -> companyRepository.findCompanyById(id))
                .flatMap(companyRepository::delete)
                .thenReturn(new CompanyDeleteResponseDto(
                        id,
                        "Компания была успешно удалена"
                ));
    }

    @Override
    public Mono<Page<CompanyResponseDto>> getAllCompanies(Pageable pageable) {
        long limit = pageable.getPageSize();
        long offset = pageable.getOffset();

        Mono<Long> totalCount = companyRepository.count();
        Flux<Company> companies = companyRepository.findAllCompaniesPaged(limit, offset);

        return totalCount.zipWith(companies.collectList())
                .map(tuple -> {
                    long total = tuple.getT1();
                    List<Company> companyList = tuple.getT2();

                    List<CompanyResponseDto> dtoList = companyList.stream()
                            .map(c ->
                                 CompanyResponseDto.builder()
                                        .id(c.getId())
                                        .name(c.getName())
                                        .email(c.getEmail())
                                        .description(c.getDescription())
                                        .build()
                            ).toList();
        return new PageImpl<>(dtoList, pageable, total);
        });
    }

    @Override
    public Mono<Boolean> existsCompanyById(UUID id) {
        return companyRepository.existsById(id);
    }


    public Mono<UserResponseDto> createRemoteUser(UserRequestDto userRequestDto){
        return Mono.deferContextual(ctx -> {
            String token = ctx.getOrDefault("authToken", null);

            CircuitBreaker cb = registry.circuitBreaker("userClientCB");

            if (token == null) return Mono.error(new BadCredentialsException("Not authorized"));

            return Mono.fromCallable(() -> {
                        return userClient.registerCompanyOwner(userRequestDto, token);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .transformDeferred(CircuitBreakerOperator.of(cb))
                    .onErrorResume(e -> createUserFallback(userRequestDto, e));
        });
    }

    public Mono<UserResponseDto> findUserById(UUID userId) {
        CircuitBreaker cb = registry.circuitBreaker("userClientCB");

        return Mono.fromCallable(() -> userClient.findUserById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .onErrorResume(this::findUserByIdFallback);
    }

    private Mono<UUID> getUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> (UserPrincipal) auth.getPrincipal())
                .map(UserPrincipal::userId);
    }

    private Company getCompany(CompanyRequestDto companyRequestDto, CompanyStatus companyStatus){
        Company company = new Company();
        company.setName(companyRequestDto.name());
        company.setEmail(companyRequestDto.email());
        company.setDescription(companyRequestDto.description());
        company.setStatusId(companyStatus.getId());
        return company;
    }

    private CompanyResponseDto mapToCompanyResponseDto(CompanyRequestDto companyRequestDto, UserCompany userCompany){
        return new CompanyResponseDto(
                userCompany.getCompanyId(),
                companyRequestDto.name(),
                companyRequestDto.email(),
                companyRequestDto.description(),
                "Компания зарегистрирована. Ожидайте проверки администратора.",
                userCompany.getUserId()
        );
    }

    public Mono<UserResponseDto> createUserFallback(UserRequestDto dto, Throwable e) {
        return Mono.error(new UserClientException(
                "User service сейчас не доступен, создание юзера невозможно",
                HttpStatus.SERVICE_UNAVAILABLE
        ));
    }

    private Mono<UserResponseDto> findUserByIdFallback(Throwable e) {
        return Mono.error(new UserClientException(
                "User service сейчас не доступен, получение юзера невозможно",
                HttpStatus.SERVICE_UNAVAILABLE
        ));
    }


}
