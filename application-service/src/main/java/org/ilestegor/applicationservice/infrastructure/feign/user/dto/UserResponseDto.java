package org.ilestegor.applicationservice.infrastructure.feign.user.dto;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String fullName,
        String email
) {
}