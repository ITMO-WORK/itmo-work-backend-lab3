package org.itmowork.vacancy_service.security;

import io.jsonwebtoken.Claims;

public interface JwtService {
    Claims parseAllClaims(String token);
}
