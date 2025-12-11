package org.ilestegor.applicationservice.configuration.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ilestegor.applicationservice.exception.ProblemDetailsUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPointHandler implements ServerAuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail problem = ProblemDetailsUtils.problemDetail(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication token is missing or invalid",
                exchange.getRequest()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (Exception ex) {
            bytes = """
                    {"status":401,"title":"Unauthorized","detail":"Invalid credentials"}
                    """.getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }


}
