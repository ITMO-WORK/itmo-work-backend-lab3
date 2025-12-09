package com.itmowork.company_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequestDto(
        @NotBlank(message = "Company name must be not blank")
        @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
        String name,

        @NotBlank(message = "Company email must be not blank")
        @Size(min = 5, max = 320, message = "Company email must be between 5 and 320 characters")
        String email,

        String description,
        String ownerFullName,
        String ownerEmail,
        String ownerPassword
) {
}
