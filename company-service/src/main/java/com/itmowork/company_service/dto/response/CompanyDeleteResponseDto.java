package com.itmowork.company_service.dto.response;

import java.util.UUID;

public record CompanyDeleteResponseDto(
        UUID id,
        String message
) {
}
