package com.itmowork.company_service.dto.response;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String token
) {
}
