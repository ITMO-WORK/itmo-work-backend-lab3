package org.ilestegor.itmoworkgateway.service.interfaces;

import io.jsonwebtoken.Claims;

public interface JwtService {
    Claims parseAllClaims(String token);
}