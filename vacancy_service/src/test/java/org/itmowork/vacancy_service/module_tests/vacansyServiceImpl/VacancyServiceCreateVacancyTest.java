//package org.itmowork.vacancy_service.module_tests.vacansyServiceImpl;
//
//import org.itmowork.vacancy_service.dto.request.VacancyCreateRequestDto;
//import org.itmowork.vacancy_service.dto.response.VacancyResponseDto;
//import org.itmowork.vacancy_service.exception.exceptions.*;
//import org.itmowork.vacancy_service.infrastructure.feign.CompanyClient;
//import org.itmowork.vacancy_service.mappers.VacancyMapper;
//import org.itmowork.vacancy_service.model.Currency;
//import org.itmowork.vacancy_service.model.Vacancy;
//import org.itmowork.vacancy_service.model.VacancyStatus;
//import org.itmowork.vacancy_service.model.VacancyStatusName;
//import org.itmowork.vacancy_service.repository.VacancyRepository;
//import org.itmowork.vacancy_service.service.VacancyServiceImpl;
//import org.itmowork.vacancy_service.service.interfaces.CurrencyService;
//import org.itmowork.vacancy_service.service.interfaces.VacancyStatusService;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.UUID;
//
//import static org.mockito.ArgumentMatchers.any;
//
//@ExtendWith(MockitoExtension.class)
//class VacancyServiceCreateVacancyTest {
//
//    @Mock
//    private VacancyRepository vacancyRepository;
//    @Mock
//    private VacancyStatusService vacancyStatusService;
//    @Mock
//    private CurrencyService currencyService;
//    @Mock
//    private CompanyClient companyClient;
//    @Mock
//    private VacancyMapper vacancyMapper;
//
//    @InjectMocks
//    private VacancyServiceImpl vacancyService;
//
//    private UUID userId;
//    private UUID companyId;
//    private VacancyCreateRequestDto request;
//
//    @BeforeEach
//    void setup() {
//        userId = UUID.randomUUID();
//        companyId = UUID.randomUUID();
//
//        request = new VacancyCreateRequestDto(
//                "Backend",
//                "We need backend dev",
//                100,
//                200,
//                companyId,
//                1L
//        );
//    }
//
//    @Test
//    void createVacancySuccess() {
//
//        Currency currency = Currency.builder().id(1L).build();
//        VacancyStatus status = VacancyStatus.builder()
//                .id(2L)
//                .vacancyStatusName(VacancyStatusName.DRAFT)
//                .build();
//
//        Vacancy savedVacancy = Vacancy.builder()
//                .id(UUID.randomUUID())
//                .title(request.title())
//                .description(request.description())
//                .salaryFrom(request.salaryFrom())
//                .salaryTo(request.salaryTo())
//                .companyId(companyId)
//                .currency(currency)
//                .status(status)
//                .build();
//
//        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
//        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(true);
//        Mockito.when(currencyService.findCurrencyById(1L)).thenReturn(currency);
//        Mockito.when(vacancyStatusService.findByVacancyStatusName(VacancyStatusName.DRAFT))
//                .thenReturn(status);
//
//        Mockito.when(vacancyRepository.save(Mockito.any()))
//                .thenReturn(savedVacancy);
//
//        VacancyResponseDto result = vacancyService.createVacancy(
//                userId, request, VacancyStatusName.DRAFT
//        );
//
//        Assertions.assertEquals(request.title(), result.title());
//        Assertions.assertEquals(request.description(), result.description());
//        Assertions.assertEquals(request.salaryFrom(), result.salaryFrom());
//        Assertions.assertEquals(request.salaryTo(), result.salaryTo());
//        Assertions.assertEquals(currency.getId(), result.currencyId());
//        Assertions.assertEquals(status.getId(), result.statusId());
//    }
//
//    @Test
//    void createVacancyCompanyDoesNotExist() {
//
//        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(false);
//
//        Assertions.assertThrows(
//                CompanyNotFoundException.class,
//                () -> vacancyService.createVacancy(userId, request, VacancyStatusName.DRAFT)
//        );
//    }
//
//    @Test
//    void createVacancyUserNotOwner() {
//
//        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
//        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId))
//                .thenReturn(false);
//
//        Assertions.assertThrows(
//                CompanyNotFoundException.class,
//                () -> vacancyService.createVacancy(userId, request, VacancyStatusName.DRAFT)
//        );
//    }
//
//    @Test
//    void createVacancyCurrencyNotFound() {
//
//        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
//        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId))
//                .thenReturn(true);
//
//        Mockito.when(currencyService.findCurrencyById(1L))
//                .thenReturn(null);
//
//        Assertions.assertThrows(
//                CurrencyNotFoundException.class,
//                () -> vacancyService.createVacancy(userId, request, VacancyStatusName.DRAFT)
//        );
//    }
//
//    @Test
//    void createVacancyInvalidSalaryBounds() {
//
//        VacancyCreateRequestDto badRequest = new VacancyCreateRequestDto(
//                "ABC",
//                "desc",
//                500,
//                100,
//                companyId,
//                1L
//        );
//
//        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
//        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId))
//                .thenReturn(true);
//
//        Mockito.when(currencyService.findCurrencyById(1L))
//                .thenReturn(Currency.builder().id(1L).build());
//
//        Mockito.when(vacancyStatusService.findByVacancyStatusName(any()))
//                .thenReturn(VacancyStatus.builder()
//                        .id(1L)
//                        .vacancyStatusName(VacancyStatusName.DRAFT)
//                        .build());
//
//        Assertions.assertThrows(
//                InvalidVacancySalaryException.class,
//                () -> vacancyService.createVacancy(userId, badRequest, VacancyStatusName.DRAFT)
//        );
//    }
//}
//
