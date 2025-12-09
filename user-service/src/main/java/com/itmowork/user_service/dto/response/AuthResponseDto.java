package com.itmowork.user_service.dto.response;

import java.util.UUID;

public record AuthResponseDto(
        UUID id,
        String token
) {}
