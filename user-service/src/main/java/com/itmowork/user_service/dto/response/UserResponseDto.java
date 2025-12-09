package com.itmowork.user_service.dto.response;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String fullName,
        String email
) {
}
