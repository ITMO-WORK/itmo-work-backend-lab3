package org.ilestegor.applicationservice.security.interfaces;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

public interface JwtService {
    String generateAccessToken(Authentication authentication, UUID userId, List<String> roles);

    String getUserNameFromToken(String token) throws IllegalArgumentException, JwtException;

    boolean validateToken(String token, UserDetails userDetails);

    Claims getClaimsFromToken(String token);
}
