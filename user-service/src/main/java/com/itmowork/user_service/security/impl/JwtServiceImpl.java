package com.itmowork.user_service.security.impl;

import com.itmowork.user_service.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

@Component
public class JwtServiceImpl implements JwtService {
    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;

    @Value("${jwt.access-ttl}")
    private Duration jwtTtl;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
        this.secretKey = null;
    }

    @Override
    public String generateAccessToken(Authentication authentication, UUID userId) {
        HashMap<String, Object> claims = new HashMap<>();
        Instant now = Instant.now();
        Instant exp = now.plus(jwtTtl);
        claims.put("userId", userId);
        return Jwts.builder()
                .claims().add(claims)
                .subject(authentication.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .and()
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String getUserNameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser().verifyWith(key).clockSkewSeconds(30).build().parseSignedClaims(token).getPayload();
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = getUserNameFromToken(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return getClaimsFromToken(token).getExpiration();
    }
}
