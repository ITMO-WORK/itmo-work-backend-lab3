package org.itmowork.vacancy_service.service.interfaces;

import org.itmowork.vacancy_service.dto.request.VacancyCreateRequestDto;
import org.itmowork.vacancy_service.dto.request.VacancyUpdateRequestDto;
import org.itmowork.vacancy_service.dto.response.VacancyResponseDto;
import org.itmowork.vacancy_service.model.Vacancy;
import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VacancyService {

    Page<VacancyResponseDto> getAllPublishedVacancies(Pageable pageable);

    VacancyResponseDto createVacancy(
            VacancyCreateRequestDto request,
            VacancyStatusName statusName
    );

    VacancyResponseDto changeStatus(
            UUID vacancyId,
            VacancyStatusName newStatus
    );

    VacancyResponseDto updateVacancy(
            UUID vacancyId,
            VacancyUpdateRequestDto dto
    );

    VacancyResponseDto updateAndChangeStatus(
            UUID vacancyId,
            VacancyUpdateRequestDto dto,
            VacancyStatusName newStatus
    );

    Vacancy getReferenceById(UUID vacancyId);
    boolean existsVacancyById(UUID id);
    VacancyStatus findCurrentVacancyStatusByVacancyId(UUID id);
    UUID findCompanyIdByVacancyId(UUID vacancyId);
    String getVacancyTitle(UUID vacancyId);
    boolean isVacancyPublished(UUID vacancyId);
}

