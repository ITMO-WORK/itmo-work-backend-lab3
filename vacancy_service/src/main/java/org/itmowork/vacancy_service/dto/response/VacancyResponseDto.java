package org.itmowork.vacancy_service.dto.response;

import lombok.Builder;
import java.util.UUID;

@Builder
public record VacancyResponseDto(
        UUID id,
        String title,
        String description,
        Integer salaryFrom,
        Integer salaryTo,
        Long statusId,
        UUID companyId,
        Long currencyId
) {}
