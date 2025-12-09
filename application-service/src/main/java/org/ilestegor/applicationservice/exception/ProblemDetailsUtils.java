package org.ilestegor.applicationservice.exception;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemDetailsUtils {
    public static ProblemDetail problemDetail(HttpStatus status, String title, String detail, ServerHttpRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle(title);
        problemDetail.setProperty("method", request.getMethod().name());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
