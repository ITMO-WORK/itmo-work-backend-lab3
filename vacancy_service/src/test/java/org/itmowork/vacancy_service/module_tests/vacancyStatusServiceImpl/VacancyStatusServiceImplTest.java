package org.itmowork.vacancy_service.module_tests.vacancyStatusServiceImpl;

import org.itmowork.vacancy_service.exception.exceptions.VacancyStatusNotFoundException;
import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.itmowork.vacancy_service.repository.VacancyStatusRepository;
import org.itmowork.vacancy_service.service.VacancyStatusServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class VacancyStatusServiceImplTest {

    @Mock
    private VacancyStatusRepository vacancyStatusRepository;

    @InjectMocks
    private VacancyStatusServiceImpl vacancyStatusService;

    @Test
    void findByVacancyStatusNameShouldReturnStatus() {
        VacancyStatusName name = VacancyStatusName.PUBLISHED;

        VacancyStatus status = VacancyStatus.builder()
                .id(1L)
                .vacancyStatusName(name)
                .build();

        Mockito.when(vacancyStatusRepository.findByVacancyStatusName(name))
                .thenReturn(Optional.of(status));

        VacancyStatus result = vacancyStatusService.findByVacancyStatusName(name);

        Assertions.assertEquals(status, result);
        Mockito.verify(vacancyStatusRepository).findByVacancyStatusName(name);
    }

    @Test
    void findByVacancyStatusNameShouldThrowNotFound() {
        VacancyStatusName name = VacancyStatusName.DRAFT;

        Mockito.when(vacancyStatusRepository.findByVacancyStatusName(name))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                VacancyStatusNotFoundException.class,
                () -> vacancyStatusService.findByVacancyStatusName(name)
        );

        Mockito.verify(vacancyStatusRepository).findByVacancyStatusName(name);
    }

    @Test
    void findVacancyStatusByIdShouldReturnStatus() {
        Long id = 10L;

        VacancyStatus status = VacancyStatus.builder()
                .id(id)
                .vacancyStatusName(VacancyStatusName.CLOSED)
                .build();

        Mockito.when(vacancyStatusRepository.findVacancyStatusById(id))
                .thenReturn(Optional.of(status));

        VacancyStatus result = vacancyStatusService.findVacancyStatusById(id);

        Assertions.assertEquals(status, result);
        Mockito.verify(vacancyStatusRepository).findVacancyStatusById(id);
    }

    @Test
    void findVacancyStatusByIdShouldThrowNotFound() {
        Long id = 999L;

        Mockito.when(vacancyStatusRepository.findVacancyStatusById(id))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(
                VacancyStatusNotFoundException.class,
                () -> vacancyStatusService.findVacancyStatusById(id)
        );

        Mockito.verify(vacancyStatusRepository).findVacancyStatusById(id);
    }
}

