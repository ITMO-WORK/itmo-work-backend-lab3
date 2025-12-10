package com.itmowork.company_service.configuration;

import java.util.UUID;

public record UserPrincipal(
        String email,
        UUID userId
) {
}
