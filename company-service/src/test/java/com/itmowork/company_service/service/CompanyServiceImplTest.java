package com.itmowork.company_service.service;

import com.itmowork.company_service.client.UserClient;
import com.itmowork.company_service.configuration.UserPrincipal;
import com.itmowork.company_service.dto.request.CompanyRequestDto;
import com.itmowork.company_service.dto.request.CompanyUpdateRequestDto;
import com.itmowork.company_service.dto.request.UserRequestDto;
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
import com.itmowork.company_service.service.interfaces.CompanyStatusService;
import com.itmowork.company_service.service.interfaces.UserCompanyService;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;



import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyStatusService companyStatusService;

    @Mock
    private UserClient userClient;

    @Mock
    private UserCompanyService userCompanyService;

    @Mock
    private CircuitBreakerRegistry registry;

    private CompanyServiceImpl service;

    private UUID userId;
    private UUID companyId;

    @BeforeEach
    void init() {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        service = new CompanyServiceImpl(
                companyRepository,
                companyStatusService,
                userClient,
                userCompanyService,
                registry
        );
    }


    @Test
    void createCompanySuccess() {
        CompanyRequestDto req = new CompanyRequestDto(
                "TestCo",
                "test@mail.com",
                "desc",
                "Owner Name",
                "owner@mail.com",
                "pass"
        );

        UserResponseDto createdUser = new UserResponseDto(
                userId,
                "dummy-token"
        );

        CompanyStatus status = new CompanyStatus(1L, CompanyStatusName.PENDING_VERIFICATION);

        Company savedCompany = new Company(
                companyId,
                "TestCo",
                "test@mail.com",
                "desc",
                1L
        );

        UserCompany userCompany = new UserCompany(5L, userId, companyId);

        when(companyRepository.existsByEmail("test@mail.com"))
                .thenReturn(Mono.just(false));

        when(companyStatusService.findCompanyStatusByCompanyStatusName(CompanyStatusName.PENDING_VERIFICATION))
                .thenReturn(Mono.just(status));

        when(userClient.registerCompanyOwner(any(), any()))
                .thenReturn(createdUser);

        when(companyRepository.save(any()))
                .thenReturn(Mono.just(savedCompany));

        when(userCompanyService.saveUserCompany(any()))
                .thenReturn(Mono.just(userCompany));


        CircuitBreaker cb = CircuitBreaker.ofDefaults("test");
        when(registry.circuitBreaker(any())).thenReturn(cb);


        Mono<CompanyResponseDto> result = service.createCompany(req)
                .contextWrite(ctx -> ctx.put("authToken", createdUser.token()));


        StepVerifier.create(result)
                .expectNextMatches(dto ->
                        dto.id().equals(companyId)
                                && dto.userId().equals(userId)
                                && dto.name().equals("TestCo")
                )
                .verifyComplete();

        verify(companyRepository).save(any());
        verify(userCompanyService).saveUserCompany(any());
        verify(userClient).registerCompanyOwner(any(), eq(createdUser.token()));
    }


    @Test
    void createCompanyCompanyAlreadyExists() {
        CompanyRequestDto req = new CompanyRequestDto(
                "TestCo",
                "test@mail.com",
                "desc",
                "Owner Name",
                "owner@mail.com",
                "pass"
        );

        when(companyRepository.existsByEmail("test@mail.com")).thenReturn(Mono.just(true));

        StepVerifier.create(service.createCompany(req))
                .expectError(CompanyAlreadyExistsException.class)
                .verify();
    }


    @Test
    void createCompanyUserServiceUnavailable() {
        CompanyRequestDto req = new CompanyRequestDto(
                "TestCo",
                "test@mail.com",
                "desc",
                "Owner Name",
                "owner@mail.com",
                "pass"
        );

        CompanyStatus st = new CompanyStatus(1L, CompanyStatusName.PENDING_VERIFICATION);

        when(companyRepository.existsByEmail("test@mail.com")).thenReturn(Mono.just(false));
        when(companyStatusService.findCompanyStatusByCompanyStatusName(CompanyStatusName.PENDING_VERIFICATION))
                .thenReturn(Mono.just(st));


        CompanyServiceImpl spyService = Mockito.spy(service);
        doReturn(Mono.error(new UserClientException(
                "User service сейчас не доступен, создание юзера невозможно",
                HttpStatus.SERVICE_UNAVAILABLE
        )))
                .when(spyService)
                .createRemoteUser(any(UserRequestDto.class));


        StepVerifier.create(spyService.createCompany(req))
                .expectError(UserClientException.class)
                .verify();
    }


    @Test
    void updateCompanySuccess() {
        CompanyUpdateRequestDto upd = new CompanyUpdateRequestDto(
                "NewName",
                "new@mail.com",
                "new desc"
        );

        Company company = new Company(companyId, "Old", "old@mail.com", "old", 1L);
        Company saved = new Company(companyId, "NewName", "new@mail.com", "new desc", 1L);

        when(userCompanyService.validateCompanyOwnership(companyId, userId))
                .thenReturn(Mono.just(true));

        when(companyRepository.findCompanyById(companyId))
                .thenReturn(Mono.just(company));

        when(companyRepository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(
                        service.updateCompany(companyId, upd)
                                .contextWrite(
                                        ReactiveSecurityContextHolder.withSecurityContext(
                                                mockSecurityContext("test@mail.com", userId)
                                        )
                                )
                )
                .expectNextMatches(dto ->
                        dto.id().equals(companyId)
                                && dto.name().equals("NewName")
                                && dto.userId().equals(userId)
                )
                .verifyComplete();
    }

    @Test
    void updateCompanyUserNotFound() {
        CompanyUpdateRequestDto upd =
                new CompanyUpdateRequestDto("a", "b@mail.com", "c");

        StepVerifier.create(service.updateCompany(companyId, upd))
                .expectComplete()
                .verify();
    }

    @Test
    void updateCompanyNotOwner() {
        CompanyUpdateRequestDto upd = new CompanyUpdateRequestDto(
                "a",
                "b@mail.com",
                "c"
        );

        when(userCompanyService.validateCompanyOwnership(companyId, userId))
                .thenReturn(Mono.just(false));

        Mono<SecurityContext> security = mockSecurityContext("test@mail.com", userId);

        StepVerifier.create(
                        service.updateCompany(companyId, upd)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(security))
                )
                .expectError(CompanyNotFoundException.class)
                .verify();
    }

    @Test
    void deleteCompanySuccess() {
        Company company = new Company(companyId, "x", "e", "d", 1L);

        when(userCompanyService.validateCompanyOwnership(companyId, userId))
                .thenReturn(Mono.just(true));

        when(companyRepository.findCompanyById(companyId))
                .thenReturn(Mono.just(company));

        when(companyRepository.delete(company))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        service.deleteCompany(companyId)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                        mockSecurityContext("mail@test.com", userId)
                                ))
                )
                .expectNextMatches(resp -> resp.id().equals(companyId))
                .verifyComplete();
    }


    @Test
    void deleteCompanyNotOwner() {

        when(userCompanyService.validateCompanyOwnership(companyId, userId))
                .thenReturn(Mono.just(false));

        StepVerifier.create(
                        service.deleteCompany(companyId)
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                        mockSecurityContext("mail@test.com", userId)
                                ))
                )
                .expectError(CompanyNotFoundException.class)
                .verify();
    }

    @Test
    void getAllCompaniesSuccess() {
        Company c1 = new Company(UUID.randomUUID(), "A", "a@mail.com", "d", 1L);
        Company c2 = new Company(UUID.randomUUID(), "B", "b@mail.com", "d", 1L);

        when(companyRepository.count()).thenReturn(Mono.just(2L));
        when(companyRepository.findAllCompaniesPaged((long) 10, (long) 0))
                .thenReturn(Flux.just(c1, c2));

        StepVerifier.create(service.getAllCompanies(org.springframework.data.domain.PageRequest.of(0, 10)))
                .expectNextMatches(page -> page.getTotalElements() == 2
                        && page.getContent().size() == 2)
                .verifyComplete();
    }


    @Test
    void existsCompanyByIdReturnsTrue() {
        when(companyRepository.existsById(companyId))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.existsCompanyById(companyId))
                .expectNext(true)
                .verifyComplete();
    }

    private Mono<SecurityContext> mockSecurityContext(String email, UUID userId) {
        UserPrincipal principal = new UserPrincipal(email, userId);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );
        return Mono.just(new SecurityContextImpl(auth));
    }

}
