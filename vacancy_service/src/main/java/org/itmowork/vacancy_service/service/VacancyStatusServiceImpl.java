package org.itmowork.vacancy_service.service;

import lombok.AllArgsConstructor;
import org.itmowork.vacancy_service.exception.exceptions.VacancyStatusNotFoundException;
import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.itmowork.vacancy_service.repository.VacancyStatusRepository;
import org.itmowork.vacancy_service.service.interfaces.VacancyStatusService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class VacancyStatusServiceImpl implements VacancyStatusService {
    private final VacancyStatusRepository vacancyStatusRepository;

    @Override
    public VacancyStatus findByVacancyStatusName(VacancyStatusName statusName) {
        return vacancyStatusRepository.findByVacancyStatusName(statusName)
                .orElseThrow(VacancyStatusNotFoundException::new);
    }

    @Override
    public VacancyStatus findVacancyStatusById(Long vacancyStatusId) {
        return vacancyStatusRepository.findVacancyStatusById(vacancyStatusId)
                .orElseThrow(VacancyStatusNotFoundException::new);
    }
}
