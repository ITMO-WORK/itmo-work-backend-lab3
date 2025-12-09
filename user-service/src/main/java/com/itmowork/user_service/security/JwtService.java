package com.itmowork.user_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateAccessToken(Authentication authentication);

    String getUserNameFromToken(String token) throws IllegalArgumentException, JwtException;

    boolean validateToken(String token, UserDetails userDetails);

    Claims getClaimsFromToken(String token);
}
