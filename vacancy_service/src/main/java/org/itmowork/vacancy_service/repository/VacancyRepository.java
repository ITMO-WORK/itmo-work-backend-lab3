package org.itmowork.vacancy_service.repository;

import org.itmowork.vacancy_service.model.Vacancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, UUID> {

    boolean existsVacanciesById(UUID id);

    @Query("select v.status.id from Vacancy v where v.id = :vacancyId")
    Long findVacancyStatusById(UUID vacancyId);

    @Query("select v.companyId from Vacancy v where v.id = :vacancyId")
    UUID findCompanyId(UUID vacancyId);

    @Query("select v from Vacancy v where v.status.vacancyStatusName = 'PUBLISHED'")
    Page<Vacancy> getAllPublished(Pageable pageable);

    @Query("select v.title from Vacancy v where v.id = :vacancyId")
    String findTitleById(UUID vacancyId);

    @Query("select case when v.status.vacancyStatusName = 'PUBLISHED' then true else false end " +
            "from Vacancy v where v.id = :vacancyId")
    Boolean isPublished(UUID vacancyId);
}

