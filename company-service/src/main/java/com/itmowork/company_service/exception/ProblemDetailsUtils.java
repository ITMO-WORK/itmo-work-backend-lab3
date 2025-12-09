package com.itmowork.company_service.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemDetailsUtils {

    public static ProblemDetail problemDetail(HttpStatus status, String title, String detail, ServerWebExchange exchange) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("method", exchange.getRequest().getMethod().name());
        pd.setProperty("path", exchange.getRequest().getPath().value());
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}