package org.itmowork.vacancy_service.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import feign.RequestInterceptor;
import jakarta.ws.rs.core.HttpHeaders;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig {

    @Bean
    public HttpMessageConverters httpMessageConverters(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(mapper);

        return new HttpMessageConverters(jacksonConverter);

    }

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return requestTemplate -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getCredentials() == null) {
                return;
            }

            String token = auth.getCredentials().toString();
            if (token == null || token.isBlank()) {
                return;
            }

            requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        };
    }
}