package org.ilestegor.applicationservice.configuration;

import feign.Logger;
import lombok.RequiredArgsConstructor;
import org.ilestegor.applicationservice.configuration.filters.JwtFilter;
import org.ilestegor.applicationservice.configuration.handler.CustomAccessDeniedHandler;
import org.ilestegor.applicationservice.configuration.handler.CustomAuthenticationEntryPointHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;

    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPointHandler customAuthenticationEntryPointHandler;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity serverHttpRequest){

        serverHttpRequest.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        serverHttpRequest.authorizeExchange(auth -> auth
                .pathMatchers(
                        "/api/auth/**",
                        "/api/user/create",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/swagger-ui/index.html",
                        "/v3/api-docs/**"
                ).permitAll()
                .anyExchange().authenticated()
        ).addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        serverHttpRequest.exceptionHandling(handler -> handler
                .authenticationEntryPoint(customAuthenticationEntryPointHandler)
                .accessDeniedHandler(customAccessDeniedHandler)
        );
        return serverHttpRequest.build();

    }
}
