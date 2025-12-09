package org.itmowork.vacancy_service.module_tests.vacansyServiceImpl;

import org.itmowork.vacancy_service.dto.request.VacancyUpdateRequestDto;
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
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class VacancyServiceUpdateAndChangeStatusTest {

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

    private Vacancy vacancyDraft;

    @BeforeEach
    void setup() {
        vacancyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        vacancyDraft = Vacancy.builder()
                .id(vacancyId)
                .companyId(companyId)
                .title("Old")
                .description("Old desc")
                .salaryFrom(100)
                .salaryTo(200)
                .currency(Currency.builder().id(1L).build())
                .status(VacancyStatus.builder()
                        .id(1L)
                        .vacancyStatusName(VacancyStatusName.DRAFT)
                        .build())
                .build();
    }

    @Test
    void updateAndChangeStatusCurrencyNotFound() {
        VacancyUpdateRequestDto dto = new VacancyUpdateRequestDto(null, null, null, null, 99L);

        Mockito.when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancyDraft));
        Mockito.when(vacancyRepository.findCompanyId(vacancyId)).thenReturn(companyId);
        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(true);
        Mockito.when(currencyService.findCurrencyById(99L)).thenThrow(new CurrencyNotFoundException(""));

        Assertions.assertThrows(
                CurrencyNotFoundException.class,
                () -> vacancyService.updateAndChangeStatus(
                        userId, vacancyId, dto,
                        VacancyStatusName.PUBLISHED
                )
        );
    }

    @Test
    void updateAndChangeStatusInvalidStatusTransition() {
        vacancyDraft.setStatus(VacancyStatus.builder()
                .vacancyStatusName(VacancyStatusName.DRAFT)
                .build());

        Mockito.when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancyDraft));
        Mockito.when(vacancyRepository.findCompanyId(vacancyId)).thenReturn(companyId);
        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(true);

        Assertions.assertThrows(
                InvalidVacancyStatusChangeException.class,
                () -> vacancyService.updateAndChangeStatus(
                        userId, vacancyId,
                        new VacancyUpdateRequestDto(null, null, null, null, null),
                        VacancyStatusName.CLOSED
                )
        );
    }

    @Test
    void updateAndChangeStatusInvalidCurrentStatus() {
        vacancyDraft.setStatus(VacancyStatus.builder()
                .vacancyStatusName(VacancyStatusName.CLOSED)
                .build());

        Mockito.when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancyDraft));
        Mockito.when(vacancyRepository.findCompanyId(vacancyId)).thenReturn(companyId);
        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(true);

        Assertions.assertThrows(
                InvalidVacancyStatusException.class,
                () -> vacancyService.updateAndChangeStatus(
                        userId, vacancyId, new VacancyUpdateRequestDto(null, null, null, null, null),
                        VacancyStatusName.PUBLISHED
                )
        );
    }

    @Test
    void updateAndChangeStatusNotOwner() {
        Mockito.when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancyDraft));
        Mockito.when(vacancyRepository.findCompanyId(vacancyId)).thenReturn(companyId);
        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId))
                .thenReturn(false);

        Assertions.assertThrows(
                CompanyNotFoundException.class,
                () -> vacancyService.updateAndChangeStatus(
                        userId, vacancyId, new VacancyUpdateRequestDto(null, null, null, null, null),
                        VacancyStatusName.PUBLISHED
                )
        );
    }

    @Test
    void updateAndChangeStatusCompanyNotExists() {
        Mockito.when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancyDraft));
        Mockito.when(vacancyRepository.findCompanyId(vacancyId)).thenReturn(companyId);
        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(false);

        Assertions.assertThrows(
                CompanyNotFoundException.class,
                () -> vacancyService.updateAndChangeStatus(
                        userId, vacancyId, new VacancyUpdateRequestDto(null, null, null, null, null),
                        VacancyStatusName.PUBLISHED
                )
        );
    }

    @Test
    void updateAndChangeStatusVacancyNotFound() {
        Mockito.when(vacancyRepository.findById(vacancyId))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                VacancyNotFoundException.class,
                () -> vacancyService.updateAndChangeStatus(
                        userId, vacancyId, new VacancyUpdateRequestDto(null, null, null, null, null),
                        VacancyStatusName.PUBLISHED
                )
        );
    }

    @Test
    void updateAndChangeStatusSuccess() {
        VacancyUpdateRequestDto dto = new VacancyUpdateRequestDto(
                "New", "New desc", 150, 300, 1L
        );

        VacancyStatus newStatus = VacancyStatus.builder()
                .id(2L)
                .vacancyStatusName(VacancyStatusName.PUBLISHED)
                .build();

        Mockito.when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.of(vacancyDraft));

        Mockito.when(vacancyRepository.findCompanyId(vacancyId)).thenReturn(companyId);
        Mockito.when(companyClient.existsCompany(companyId)).thenReturn(true);
        Mockito.when(companyClient.validateCompanyOwnership(companyId, userId)).thenReturn(true);
        Mockito.when(currencyService.findCurrencyById(1L)).thenReturn(Currency.builder().id(1L).build());
        Mockito.when(vacancyStatusService.findByVacancyStatusName(VacancyStatusName.PUBLISHED))
                .thenReturn(newStatus);

        Mockito.when(vacancyRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        doAnswer(inv -> {
            Vacancy v = inv.getArgument(0);
            VacancyUpdateRequestDto d = inv.getArgument(1);

            if (d.title() != null) v.setTitle(d.title());
            if (d.description() != null) v.setDescription(d.description());
            if (d.salaryFrom() != null) v.setSalaryFrom(d.salaryFrom());
            if (d.salaryTo() != null) v.setSalaryTo(d.salaryTo());
            return null;
        }).when(vacancyMapper).update(any(Vacancy.class), any(VacancyUpdateRequestDto.class));

        VacancyResponseDto result = vacancyService.updateAndChangeStatus(
                userId, vacancyId, dto, VacancyStatusName.PUBLISHED
        );

        Assertions.assertEquals("New", result.title());
        Assertions.assertEquals("New desc", result.description());
        Assertions.assertEquals(150, result.salaryFrom());
        Assertions.assertEquals(300, result.salaryTo());
        Assertions.assertEquals(newStatus.getId(), result.statusId());
    }
}