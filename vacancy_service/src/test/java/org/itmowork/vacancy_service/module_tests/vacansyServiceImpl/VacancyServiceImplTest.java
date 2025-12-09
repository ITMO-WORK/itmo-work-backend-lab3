package org.itmowork.vacancy_service.module_tests.vacansyServiceImpl;

import org.itmowork.vacancy_service.dto.response.VacancyResponseDto;
import org.itmowork.vacancy_service.exception.exceptions.InvalidVacancySalaryException;
import org.itmowork.vacancy_service.exception.exceptions.VacancyNotFoundException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class VacancyServiceImplTest {

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

    @Test
    void getAllPublishedVacanciesShouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Vacancy vacancy = Vacancy.builder()
                .id(UUID.randomUUID())
                .title("Backend Developer")
                .description("Spring + Microservices")
                .salaryFrom(100)
                .salaryTo(200)
                .status(
                        VacancyStatus.builder()
                                .id(5L)
                                .vacancyStatusName(VacancyStatusName.PUBLISHED)
                                .build()
                )
                .companyId(UUID.randomUUID())
                .currency(Currency.builder().id(1L).currency("RUB").build())
                .build();

        Page<Vacancy> vacancyPage = new PageImpl<>(List.of(vacancy), pageable, 1);

        Mockito.when(vacancyRepository.getAllPublished(pageable))
                .thenReturn(vacancyPage);

        Page<VacancyResponseDto> result = vacancyService.getAllPublishedVacancies(pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        VacancyResponseDto dto = result.getContent().get(0);

        Assertions.assertEquals(vacancy.getId(), dto.id());
        Assertions.assertEquals(vacancy.getTitle(), dto.title());
        Assertions.assertEquals(vacancy.getDescription(), dto.description());
        Assertions.assertEquals(vacancy.getSalaryFrom(), dto.salaryFrom());
        Assertions.assertEquals(vacancy.getSalaryTo(), dto.salaryTo());
        Assertions.assertEquals(vacancy.getStatus().getId(), dto.statusId());
        Assertions.assertEquals(vacancy.getCompanyId(), dto.companyId());
        Assertions.assertEquals(vacancy.getCurrency().getId(), dto.currencyId());

        Mockito.verify(vacancyRepository).getAllPublished(pageable);
    }

    @Test
    void getReferenceByIdShouldReturnFromRepository() {
        UUID id = UUID.randomUUID();
        Vacancy vacancy = new Vacancy();
        Mockito.when(vacancyRepository.getReferenceById(id)).thenReturn(vacancy);

        Vacancy result = vacancyService.getReferenceById(id);

        Assertions.assertSame(vacancy, result);
        Mockito.verify(vacancyRepository).getReferenceById(id);
    }

    @Test
    void existsVacancyByIdShouldReturnTrue() {
        UUID id = UUID.randomUUID();
        Mockito.when(vacancyRepository.existsVacanciesById(id)).thenReturn(true);

        boolean result = vacancyService.existsVacancyById(id);

        Assertions.assertTrue(result);
        Mockito.verify(vacancyRepository).existsVacanciesById(id);
    }

    @Test
    void findCurrentVacancyStatusByVacancyIdShouldReturnStatus() {
        UUID id = UUID.randomUUID();
        Long statusId = 10L;

        VacancyStatus status = VacancyStatus.builder()
                .id(statusId)
                .vacancyStatusName(VacancyStatusName.DRAFT)
                .build();

        Mockito.when(vacancyRepository.findVacancyStatusById(id)).thenReturn(statusId);
        Mockito.when(vacancyStatusService.findVacancyStatusById(statusId)).thenReturn(status);

        VacancyStatus result = vacancyService.findCurrentVacancyStatusByVacancyId(id);

        Assertions.assertEquals(status, result);
        Mockito.verify(vacancyRepository).findVacancyStatusById(id);
        Mockito.verify(vacancyStatusService).findVacancyStatusById(statusId);
    }

    @Test
    void findCompanyIdByVacancyIdShouldReturnCompanyId() {
        UUID vacId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Mockito.when(vacancyRepository.findCompanyId(vacId)).thenReturn(companyId);

        UUID result = vacancyService.findCompanyIdByVacancyId(vacId);

        Assertions.assertEquals(companyId, result);
        Mockito.verify(vacancyRepository).findCompanyId(vacId);
    }

    @Test
    void findCompanyIdByVacancyIdShouldThrowNotFound() {
        UUID vacId = UUID.randomUUID();

        Mockito.when(vacancyRepository.findCompanyId(vacId)).thenReturn(null);

        Assertions.assertThrows(
                VacancyNotFoundException.class,
                () -> vacancyService.findCompanyIdByVacancyId(vacId)
        );

        Mockito.verify(vacancyRepository).findCompanyId(vacId);
    }

    @Test
    void validateSalaryBoundsShouldThrowWhenSalaryFromNegative() {
        Assertions.assertThrows(
                InvalidVacancySalaryException.class,
                () -> invokeValidateSalaryBounds(-1, 100)
        );
    }

    @Test
    void validateSalaryBoundsShouldThrowWhenSalaryToNegative() {
        Assertions.assertThrows(
                InvalidVacancySalaryException.class,
                () -> invokeValidateSalaryBounds(10, -5)
        );
    }

    @Test
    void validateSalaryBoundsShouldThrowWhenFromGreaterThanTo() {
        Assertions.assertThrows(
                InvalidVacancySalaryException.class,
                () -> invokeValidateSalaryBounds(200, 100)
        );
    }

    @Test
    void validateSalaryBoundsValidCaseShouldPass() {
        invokeValidateSalaryBounds(10, 100);
    }

    private void invokeValidateSalaryBounds(Integer from, Integer to) {
        try {
            var method = VacancyServiceImpl.class
                    .getDeclaredMethod("validateSalaryBounds", Integer.class, Integer.class);
            method.setAccessible(true);
            method.invoke(vacancyService, from, to);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(e);
        }
    }

    @Test
    void getVacancyTitleShouldReturnTitle() {
        UUID id = UUID.randomUUID();
        Mockito.when(vacancyRepository.findTitleById(id)).thenReturn("Java Dev");

        String result = vacancyService.getVacancyTitle(id);

        Assertions.assertEquals("Java Dev", result);
        Mockito.verify(vacancyRepository).findTitleById(id);
    }

    @Test
    void getVacancyTitleShouldThrowWhenNull() {
        UUID id = UUID.randomUUID();
        Mockito.when(vacancyRepository.findTitleById(id)).thenReturn(null);

        Assertions.assertThrows(
                VacancyNotFoundException.class,
                () -> vacancyService.getVacancyTitle(id)
        );
    }

    @Test
    void isVacancyPublishedShouldReturnTrue() {
        UUID id = UUID.randomUUID();
        Mockito.when(vacancyRepository.isPublished(id)).thenReturn(true);

        boolean result = vacancyService.isVacancyPublished(id);

        Assertions.assertTrue(result);
    }

    @Test
    void isVacancyPublishedShouldReturnFalse() {
        UUID id = UUID.randomUUID();
        Mockito.when(vacancyRepository.isPublished(id)).thenReturn(false);

        boolean result = vacancyService.isVacancyPublished(id);

        Assertions.assertFalse(result);
    }

    @Test
    void isVacancyPublishedShouldThrowWhenNull() {
        UUID id = UUID.randomUUID();
        Mockito.when(vacancyRepository.isPublished(id)).thenReturn(null);

        Assertions.assertThrows(
                VacancyNotFoundException.class,
                () -> vacancyService.isVacancyPublished(id)
        );
    }
}

