package org.ilestegor.applicationservice.exception;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemDetailsUtils {
    public static ProblemDetail problemDetail(HttpStatus status, String title, String detail, ServerWebExchange exchange) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle(title);
        problemDetail.setProperty("method", exchange.getRequest().getMethod().name());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
