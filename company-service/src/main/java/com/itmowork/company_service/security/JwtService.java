package com.itmowork.company_service.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface JwtService {

    Claims parseAllClaims(String token);

    String generateAccessToken(Authentication authentication, UUID userId, List<String> roles);
}
