package org.ilestegor.itmoworkgateway.config.filter;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.ilestegor.itmoworkgateway.service.interfaces.JwtService;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter implements WebFilter, Ordered {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.parseAllClaims(token);
            String email = claims.getSubject();
            String userId = claims.get("userId", String.class);
            List<String> roles = claims.get("roles", List.class);

            if (email == null || email.isBlank() || userId == null) {
                return unauthorized(exchange);
            }

            List<SimpleGrantedAuthority> authorities =
                    roles == null
                            ? List.of()
                            : (List<SimpleGrantedAuthority>) roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

            // principal = email, credentials = token (JWT)
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    email,
                    token,
                    authorities
            );

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

