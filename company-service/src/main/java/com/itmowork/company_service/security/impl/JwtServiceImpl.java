package com.itmowork.company_service.security.impl;

import com.itmowork.company_service.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class JwtServiceImpl implements JwtService {


    private final SecretKey key;
    @Value("${jwt.access-ttl}")
    private Duration jwtTtl;

    public JwtServiceImpl(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    @Override
    public Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public String generateAccessToken(Authentication authentication, UUID userId, List<String> roles) {
        HashMap<String, Object> claims = new HashMap<>();
        Instant now = Instant.now();
        Instant exp = now.plus(jwtTtl);
        claims.put("userId", userId);
        claims.put("roles", roles);
        return Jwts.builder()
                .claims().add(claims)
                .subject(authentication.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .and()
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
