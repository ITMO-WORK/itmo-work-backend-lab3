package com.itmowork.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface JwtService {

    String generateAccessToken(Authentication authentication, UUID userId);

    String getUserNameFromToken(String token) throws IllegalArgumentException, JwtException;

    boolean validateToken(String token, UserDetails userDetails);

    Claims getClaimsFromToken(String token);
}
