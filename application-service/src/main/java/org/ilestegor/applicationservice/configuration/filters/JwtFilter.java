package org.ilestegor.applicationservice.configuration.filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ilestegor.applicationservice.configuration.UserPrincipal;
import org.ilestegor.applicationservice.security.interfaces.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")){
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.getClaimsFromToken(token);
            String email = claims.getSubject();
            UUID userId = UUID.fromString((String) claims.get("userId"));
            List<String> roles = ((List<?>) claims.get("roles"))
                    .stream()
                    .map(Object::toString)
                    .toList();
            List<GrantedAuthority> authorityList =  roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            if (email == null || email.isBlank())
                return chain.filter(exchange);

            Authentication authentication = new UsernamePasswordAuthenticationToken(new UserPrincipal(email, userId), null, authorityList);
            return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))));
        } catch (JwtException | IllegalArgumentException ex){
            return chain.filter(exchange);
        }
    }
}
