package org.itmowork.vacancy_service.service.interfaces;

import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;

public interface VacancyStatusService {
    VacancyStatus findByVacancyStatusName(VacancyStatusName statusName);
    VacancyStatus findVacancyStatusById(Long vacancyStatusId);
}

