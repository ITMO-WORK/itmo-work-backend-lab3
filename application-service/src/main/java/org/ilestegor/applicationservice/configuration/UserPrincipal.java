package org.ilestegor.applicationservice.configuration;

import java.util.UUID;

public record UserPrincipal (
        String email,
        UUID userId
){ }
