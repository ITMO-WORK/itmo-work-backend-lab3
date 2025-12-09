package org.itmowork.vacancy_service.module_tests.vacansyServiceImpl;

import org.itmowork.vacancy_service.dto.response.VacancyResponseDto;
import org.itmowork.vacancy_service.exception.exceptions.*;
import org.itmowork.vacancy_service.infrastructure.feign.CompanyClient;
import org.itmowork.vacancy_service.mappers.VacancyMapper;
import org.itmowork.vacancy_service.model.Currency;
import org.itmowork.vacancy_service.model.Vacancy;
import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.itmowork.vacancy_service.repository.VacancyRepository;
import org.itmowork.vacancy_service.service.VacancyServiceImpl;
import org.itmowork.vacancy_service.service.interfaces.CurrencyService;
import org.itmowork.vacancy_service.service.interfaces.VacancyStatusService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class VacancyServiceChangeStatusTest {

    @Mock
    private VacancyRepository vacancyRepository;
    @Mock
    private VacancyStatusService vacancyStatusService;
    @Mock
    private CurrencyService currencyService;
    @Mock
    private CompanyClient companyClient;
    @Mock
    private VacancyMapper vacancyMapper;

    @InjectMocks
    private VacancyServiceImpl vacancyService;

    private UUID vacancyId;
    private UUID userId;
    private UUID companyId;

    private Vacancy vacancy;

    @BeforeEach
    void setup() {
        vacancyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        vacancy = Vacancy.builder()
                .id(vacancyId)
                .companyId(companyId)
                .salaryFrom(100)
                .salaryTo(200)
                .status(VacancyStatus.builder()
                        .id(1L)
                        .vacancyStatusName(VacancyStatusName.DRAFT)
                        .build())
                .currency(Currency.builder().id(1L).build())
                .build();
    }

    @Test
    void changeStatusSuccess() {

        VacancyStatus statusPublished = VacancyStatus.builder()
                .id(2L)
                .vacancyStatusName(VacancyStatusName.PUBLISHED)
                .build();

        Mockito.when(vacancyRepository.findById(vacancyId))
                .thenReturn(Optional.of(vacancy));

        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(true);

        Mockito.when(vacancyStatusService.findByVacancyStatusName(VacancyStatusName.PUBLISHED))
                .thenReturn(statusPublished);

        Mockito.when(vacancyRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        VacancyResponseDto result = vacancyService.changeStatus(
                userId, vacancyId, VacancyStatusName.PUBLISHED
        );

        Assertions.assertEquals(VacancyStatusName.PUBLISHED, statusPublished.getVacancyStatusName());
        Assertions.assertEquals(statusPublished.getId(), result.statusId());
    }

    @Test
    void changeStatusVacancyNotFound() {

        Mockito.when(vacancyRepository.findById(vacancyId))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                VacancyNotFoundException.class,
                () -> vacancyService.changeStatus(userId, vacancyId, VacancyStatusName.PUBLISHED)
        );
    }

    @Test
    void changeStatusCompanyNotExists() {

        Mockito.when(vacancyRepository.findById(vacancyId))
                .thenReturn(Optional.of(vacancy));

        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(false);

        Assertions.assertThrows(
                CompanyNotFoundException.class,
                () -> vacancyService.changeStatus(userId, vacancyId, VacancyStatusName.PUBLISHED)
        );
    }

    @Test
    void changeStatusUserNotOwner() {

        Mockito.when(vacancyRepository.findById(vacancyId))
                .thenReturn(Optional.of(vacancy));

        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(false);

        Assertions.assertThrows(
                CompanyNotFoundException.class,
                () -> vacancyService.changeStatus(userId, vacancyId, VacancyStatusName.PUBLISHED)
        );
    }

    @Test
    void changeStatusInvalidTransition() {

        vacancy.setStatus(
                VacancyStatus.builder()
                        .id(3L)
                        .vacancyStatusName(VacancyStatusName.CLOSED)
                        .build()
        );

        Mockito.when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancy));
        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(true);

        Assertions.assertThrows(
                InvalidVacancyStatusChangeException.class,
                () -> vacancyService.changeStatus(userId, vacancyId, VacancyStatusName.PUBLISHED)
        );
    }
}

