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

    VacancyResponseDto updateAndChangeStatus(
            UUID userId,
            UUID vacancyId,
            VacancyUpdateRequestDto dto,
            VacancyStatusName newStatus
    );

    VacancyResponseDto updateVacancy(
            UUID userId,
            UUID vacancyId,
            VacancyUpdateRequestDto dto
    );

    VacancyResponseDto changeStatus(
            UUID userId,
            UUID vacancyId,
            VacancyStatusName newStatus
    );

    VacancyResponseDto createVacancy(
            UUID userId,
            VacancyCreateRequestDto request,
            VacancyStatusName statusName
    );

    Vacancy getReferenceById(UUID vacancyId);
    boolean existsVacancyById(UUID id);
    VacancyStatus findCurrentVacancyStatusByVacancyId(UUID id);
    UUID findCompanyIdByVacancyId(UUID vacancyId);
    Page<VacancyResponseDto> getAllPublishedVacancies(Pageable pageable);
    String getVacancyTitle(UUID vacancyId);
    boolean isVacancyPublished(UUID vacancyId);
}

