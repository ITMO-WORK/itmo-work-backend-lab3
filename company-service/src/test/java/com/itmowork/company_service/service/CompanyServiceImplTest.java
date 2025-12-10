//package com.itmowork.company_service.service;
//
//import com.itmowork.company_service.client.UserClient;
//import com.itmowork.company_service.dto.request.CompanyRequestDto;
//import com.itmowork.company_service.dto.request.CompanyUpdateRequestDto;
//import com.itmowork.company_service.dto.request.UserRequestDto;
//import com.itmowork.company_service.dto.response.UserResponseDto;
//import com.itmowork.company_service.exception.exceptions.CompanyAlreadyExistsException;
//import com.itmowork.company_service.exception.exceptions.CompanyNotFoundException;
//import com.itmowork.company_service.exception.exceptions.UserClientException;
//import com.itmowork.company_service.model.Company;
//import com.itmowork.company_service.model.CompanyStatus;
//import com.itmowork.company_service.model.CompanyStatusName;
//import com.itmowork.company_service.model.UserCompany;
//import com.itmowork.company_service.repository.CompanyRepository;
//import com.itmowork.company_service.service.interfaces.CompanyStatusService;
//import com.itmowork.company_service.service.interfaces.UserCompanyService;
//
//import feign.FeignException;
//import feign.Request;
//import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import org.springframework.http.HttpStatus;
//import reactor.core.publisher.Mono;
//import reactor.core.publisher.Flux;
//import reactor.test.StepVerifier;
//
//
//import java.util.HashMap;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class CompanyServiceImplTest {
//
//    @Mock
//    private CompanyRepository companyRepository;
//
//    @Mock
//    private CompanyStatusService companyStatusService;
//
//    @Mock
//    private UserClient userClient;
//
//    @Mock
//    private UserCompanyService userCompanyService;
//
//    @Mock
//    private CircuitBreakerRegistry registry;
//
//    private CompanyServiceImpl service;
//
//    private UUID userId;
//    private UUID companyId;
//
//    @BeforeEach
//    void init() {
//        userId = UUID.randomUUID();
//        companyId = UUID.randomUUID();
//
//        service = new CompanyServiceImpl(
//                companyRepository,
//                companyStatusService,
//                userClient,
//                userCompanyService,
//                registry
//        );
//    }
//
//
//    @Test
//    void createCompanySuccess() {
//        CompanyRequestDto req = new CompanyRequestDto(
//                "TestCo",
//                "test@mail.com",
//                "desc",
//                "Owner Name",
//                "owner@mail.com",
//                "pass"
//        );
//
//        UserResponseDto createdUser = new UserResponseDto(
//                userId, "Owner Name", "owner@mail.com"
//        );
//
//        CompanyStatus st = new CompanyStatus(1L, CompanyStatusName.PENDING_VERIFICATION);
//
//        Company savedCompany = new Company(companyId, "TestCo", "test@mail.com", "desc", 1L);
//
//        UserCompany uc = new UserCompany(1L, userId, companyId);
//
//        when(companyRepository.existsByEmail("test@mail.com")).thenReturn(Mono.just(false));
//        when(companyStatusService.findCompanyStatusByCompanyStatusName(CompanyStatusName.PENDING_VERIFICATION))
//                .thenReturn(Mono.just(st));
//
//        CompanyServiceImpl spyService = Mockito.spy(service);
//
//        doReturn(Mono.just(createdUser))
//                .when(spyService)
//                .createRemoteUser(any(UserRequestDto.class));
//
//        when(companyRepository.save(any(Company.class))).thenReturn(Mono.just(savedCompany));
//        when(userCompanyService.saveUserCompany(any(UserCompany.class))).thenReturn(Mono.just(uc));
//
//        StepVerifier.create(spyService.createCompany(req))
//                .expectNextMatches(dto ->
//                        dto.id().equals(companyId)
//                                && dto.userId().equals(userId)
//                                && dto.name().equals("TestCo")
//                )
//                .verifyComplete();
//
//        verify(spyService).createRemoteUser(any());
//        verify(companyRepository).save(any());
//    }
//
//    @Test
//    void createCompanyCompanyAlreadyExists() {
//        CompanyRequestDto req = new CompanyRequestDto(
//                "TestCo",
//                "test@mail.com",
//                "desc",
//                "Owner Name",
//                "owner@mail.com",
//                "pass"
//        );
//
//        when(companyRepository.existsByEmail("test@mail.com")).thenReturn(Mono.just(true));
//
//        StepVerifier.create(service.createCompany(req))
//                .expectError(CompanyAlreadyExistsException.class)
//                .verify();
//    }
//
//
//    @Test
//    void createCompanyUserServiceUnavailable() {
//        CompanyRequestDto req = new CompanyRequestDto(
//                "TestCo",
//                "test@mail.com",
//                "desc",
//                "Owner Name",
//                "owner@mail.com",
//                "pass"
//        );
//
//        CompanyStatus st = new CompanyStatus(1L, CompanyStatusName.PENDING_VERIFICATION);
//
//        when(companyRepository.existsByEmail("test@mail.com")).thenReturn(Mono.just(false));
//        when(companyStatusService.findCompanyStatusByCompanyStatusName(CompanyStatusName.PENDING_VERIFICATION))
//                .thenReturn(Mono.just(st));
//
//
//        CompanyServiceImpl spyService = Mockito.spy(service);
//        doReturn(Mono.error(new UserClientException(
//                "User service сейчас не доступен, создание юзера невозможно",
//                HttpStatus.SERVICE_UNAVAILABLE
//        )))
//                .when(spyService)
//                .createRemoteUser(any(UserRequestDto.class));
//
//
//        StepVerifier.create(spyService.createCompany(req))
//                .expectError(UserClientException.class)
//                .verify();
//    }
//
//
//    @Test
//    void updateCompanySuccess() {
//        CompanyUpdateRequestDto upd = new CompanyUpdateRequestDto(
//                "NewName",
//                "new@mail.com",
//                "new desc"
//        );
//
//        Company company = new Company(companyId, "Old", "old@mail.com", "old", 1L);
//        Company saved = new Company(companyId, "NewName", "new@mail.com", "new desc", 1L);
//
//        CompanyServiceImpl spyService = Mockito.spy(service);
//
//        doReturn(Mono.just(new UserResponseDto(userId, "User", "u@mail.com")))
//                .when(spyService)
//                .findUserById(userId);
//
//        when(userCompanyService.validateCompanyOwnership(companyId, userId))
//                .thenReturn(Mono.just(true));
//
//        when(companyRepository.findCompanyById(companyId))
//                .thenReturn(Mono.just(company));
//
//        when(companyRepository.save(any())).thenReturn(Mono.just(saved));
//
//
//        StepVerifier.create(spyService.updateCompany(companyId, userId, upd))
//                .expectNextMatches(dto ->
//                        dto.id().equals(companyId)
//                                && dto.name().equals("NewName")
//                )
//                .verifyComplete();
//    }
//
//    @Test
//    void updateCompanyUserNotFound() {
//        CompanyUpdateRequestDto upd = new CompanyUpdateRequestDto("a", "b@mail.com", "c");
//
//        CompanyServiceImpl spyService = Mockito.spy(service);
//
//
//        Request fakeRequest = Request.create(
//                Request.HttpMethod.GET,
//                "http://example.com",
//                new HashMap<>(),
//                null,
//                new feign.RequestTemplate()
//        );
//
//
//        doReturn(Mono.error(new FeignException.NotFound(
//                "User not found",
//                fakeRequest,
//                new byte[128],
//                null
//        )))
//                .when(spyService)
//                .findUserById(userId);
//
//        StepVerifier.create(spyService.updateCompany(companyId, userId, upd))
//                .expectErrorSatisfies(error -> {
//                    assertThat(error).isInstanceOf(UserClientException.class);
//                    assertThat(error.getMessage()).contains("Пользователь " + userId + " не найден");
//                })
//                .verify();
//    }
//
//    @Test
//    void updateCompanyNotOwner() {
//        CompanyUpdateRequestDto upd = new CompanyUpdateRequestDto("a", "b@mail.com", "c");
//
//        CompanyServiceImpl spyService = Mockito.spy(service);
//
//        doReturn(Mono.just(new UserResponseDto(userId, "User", "mail@test.com")))
//                .when(spyService)
//                .findUserById(userId);
//
//        when(userCompanyService.validateCompanyOwnership(companyId, userId))
//                .thenReturn(Mono.just(false));
//
//        StepVerifier.create(spyService.updateCompany(companyId, userId, upd))
//                .expectError(CompanyNotFoundException.class)
//                .verify();
//    }
//
//
//    @Test
//    void deleteCompanySuccess() {
//        Company company = new Company(companyId, "x", "e", "d", 1L);
//
//        CompanyServiceImpl spyService = Mockito.spy(service);
//
//        doReturn(Mono.just(new UserResponseDto(userId, "User", "mail@test.com")))
//                .when(spyService)
//                .findUserById(userId);
//
//        when(userCompanyService.validateCompanyOwnership(companyId, userId))
//                .thenReturn(Mono.just(true));
//
//        when(companyRepository.findCompanyById(companyId))
//                .thenReturn(Mono.just(company));
//
//        when(companyRepository.delete(company)).thenReturn(Mono.empty());
//
//        StepVerifier.create(spyService.deleteCompany(companyId, userId))
//                .expectNextMatches(resp -> resp.id().equals(companyId))
//                .verifyComplete();
//    }
//
//    @Test
//    void deleteCompanyNotOwner() {
//        CompanyServiceImpl spyService = Mockito.spy(service);
//
//        doReturn(Mono.just(new UserResponseDto(userId, "User", "mail@test.com")))
//                .when(spyService)
//                .findUserById(userId);
//
//        when(userCompanyService.validateCompanyOwnership(companyId, userId))
//                .thenReturn(Mono.just(false));
//
//        StepVerifier.create(spyService.deleteCompany(companyId, userId))
//                .expectError(CompanyNotFoundException.class)
//                .verify();
//    }
//
//    @Test
//    void getAllCompaniesSuccess() {
//        Company c1 = new Company(UUID.randomUUID(), "A", "a@mail.com", "d", 1L);
//        Company c2 = new Company(UUID.randomUUID(), "B", "b@mail.com", "d", 1L);
//
//        when(companyRepository.count()).thenReturn(Mono.just(2L));
//        when(companyRepository.findAllCompaniesPaged((long) 10, (long) 0))
//                .thenReturn(Flux.just(c1, c2));
//
//        StepVerifier.create(service.getAllCompanies(org.springframework.data.domain.PageRequest.of(0, 10)))
//                .expectNextMatches(page -> page.getTotalElements() == 2
//                        && page.getContent().size() == 2)
//                .verifyComplete();
//    }
//
//
//    @Test
//    void existsCompanyByIdReturnsTrue() {
//        when(companyRepository.existsById(companyId))
//                .thenReturn(Mono.just(true));
//
//        StepVerifier.create(service.existsCompanyById(companyId))
//                .expectNext(true)
//                .verifyComplete();
//    }
//
//}
