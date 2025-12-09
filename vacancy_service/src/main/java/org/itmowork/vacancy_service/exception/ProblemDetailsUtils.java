package org.itmowork.vacancy_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemDetailsUtils {

    public static ProblemDetail problemDetail(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

        problemDetail.setTitle(title);
        problemDetail.setProperty("method", request.getMethod());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }

}