package org.itmowork.vacancy_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record VacancyUpdateRequestDto(
        @Size(max = 255, message = "Vacancy title must be between 0 and 255 characters")
        String title,
        String description,
        Integer salaryFrom,
        Integer salaryTo,
        Long currencyId
) {}
