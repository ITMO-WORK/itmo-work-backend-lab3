package org.ilestegor.itmoworkgateway.config.filter;

import org.ilestegor.itmoworkgateway.service.interfaces.JwtService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;

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

        Claims claims;
        try {
            claims = jwtService.parseAllClaims(token);
        } catch (Exception ex) {
            return unauthorized(exchange, "Invalid or expired JWT");
        }

//        String email = claims.getSubject();
//        String roles = (String) claims.get("roles");
//        String userId = (String) claims.get("userId");

//        var mutatedRequest = exchange.getRequest()
//                .mutate()
//                .header("X-User-Id", userId)
//                .header("X-User-Email", email)
//                .header("X-User-Roles", roles)
//                .build();

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
