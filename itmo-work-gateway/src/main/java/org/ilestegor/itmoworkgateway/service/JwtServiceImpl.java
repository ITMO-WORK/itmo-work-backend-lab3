package org.ilestegor.itmoworkgateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.ilestegor.itmoworkgateway.service.interfaces.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

@Component
public class JwtServiceImpl implements JwtService {

    private final SecretKey key;

    @Value("${jwt.access-ttl}")
    private Duration jwtTtl;

    public JwtServiceImpl(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    @Override
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwtTtl);

        return Jwts.builder()
                .claims().empty()
                .subject(authentication.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .and()
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
