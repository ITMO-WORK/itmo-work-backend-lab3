package com.itmowork.user_service.configuration.jwtConfiguration;

import com.itmowork.user_service.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
public class JwtFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return chain.filter(exchange);

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.getClaimsFromToken(token);
            String email = claims.getSubject();

            if (email == null || email.isBlank())
                return chain.filter(exchange);

            List<String> roles = claims.get("roles", List.class);
            roles.stream().forEach(System.out::println);
            List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            SecurityContextImpl context = new SecurityContextImpl(authentication);

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));

        } catch (JwtException | IllegalArgumentException ex){
            return chain.filter(exchange);
        }
    }
}
