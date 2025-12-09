package org.ilestegor.itmoworkgateway.service.interfaces;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;

public interface JwtService {
    String generateToken(Authentication authentication); // не используется в gateway
    Claims parseAllClaims(String token);
}