package com.itmowork.company_service.exception;

import com.itmowork.company_service.exception.exceptions.CompanyAlreadyExistsException;
import com.itmowork.company_service.exception.exceptions.CompanyNotFoundException;
import com.itmowork.company_service.exception.exceptions.CompanyStatusNotFoundException;
import com.itmowork.company_service.exception.exceptions.UserClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidationException(WebExchangeBindException ex,
                                                         ServerWebExchange exchange) {
        String details = ex.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return Mono.just(ProblemDetailsUtils.problemDetail(HttpStatus.BAD_REQUEST, "Validation failed", details, exchange));
    }

    @ExceptionHandler(CompanyAlreadyExistsException.class)
    public Mono<ProblemDetail> handleCompanyAlreadyExistsException(CompanyAlreadyExistsException ex, ServerWebExchange exchange){

        return Mono.just(ProblemDetailsUtils.problemDetail(HttpStatus.CONFLICT, "Company already exists", ex.getMessage(), exchange));
    }

    @ExceptionHandler(CompanyStatusNotFoundException.class)
    public Mono<ProblemDetail> handleCompanyStatusNotFoundException(CompanyStatusNotFoundException ex, ServerWebExchange exchange){

        return Mono.just(ProblemDetailsUtils.problemDetail(HttpStatus.NOT_FOUND, "Company status not found", ex.getMessage(), exchange));
    }

    @ExceptionHandler(UserClientException.class)
    public Mono<ProblemDetail> handleUserClientException(UserClientException ex, ServerWebExchange exchange){
        return Mono.just(ProblemDetailsUtils.problemDetail(ex.getStatus(), "User remote client exception", ex.getMessage(), exchange));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public Mono<ProblemDetail> handleCompanyNotFoundException(CompanyNotFoundException ex, ServerWebExchange exchange){

        return Mono.just(ProblemDetailsUtils.problemDetail(HttpStatus.NOT_FOUND, "Company not found", ex.getMessage(), exchange));
    }
}
