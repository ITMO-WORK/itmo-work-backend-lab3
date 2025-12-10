package com.itmowork.company_service.configuration;

import com.itmowork.company_service.configuration.handler.CustomAuthenticationEntryPointHandler;
import com.itmowork.company_service.configuration.jwtConfiguration.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;
    private final CustomAuthenticationEntryPointHandler customAuthenticationEntryPointHandler;


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity serverHttpRequest){

        serverHttpRequest.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        serverHttpRequest.exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPointHandler)
        );

        serverHttpRequest.authorizeExchange(auth -> auth
                .pathMatchers(HttpMethod.GET, "/api/company").permitAll()
                .pathMatchers(
                        "/api/company/register-company",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()
                .pathMatchers("/api/company/update/*")
                .hasAnyRole("ADMIN", "COMPANY_OWNER")
                .pathMatchers("/api/company/delete/*")
                .hasAnyRole("ADMIN", "COMPANY_OWNER")
                .anyExchange().authenticated()
        ).addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return serverHttpRequest.build();

    }
}
