package org.itmowork.vacancy_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.util.UUID;

@Builder
public record VacancyCreateRequestDto(
        @NotBlank(message = "Vacancy title must be not blank")
        @Size(max = 255, message = "Vacancy title must be between 0 and 255 characters")
        String title,

        String description,

        @PositiveOrZero(message = "salaryFrom must be greater than or equal to 0")
        Integer salaryFrom,

        @PositiveOrZero(message = "salaryTo must be greater than or equal to 0")
        Integer salaryTo,

        @NotNull(message = "companyId must be provided")
        UUID companyId,

        @NotNull(message = "currencyId must be provided")
        Long currencyId
) {}
