package org.itmowork.vacancy_service.repository;

import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VacancyStatusRepository extends JpaRepository<VacancyStatus, Long> {
    Optional<VacancyStatus> findByVacancyStatusName(VacancyStatusName statusName);
    Optional<VacancyStatus> findVacancyStatusById(Long vacancyStatusId);
}
