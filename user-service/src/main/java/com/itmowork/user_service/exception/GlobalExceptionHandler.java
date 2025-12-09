package com.itmowork.user_service.exception;

import com.itmowork.user_service.exception.exceptions.UserAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.management.relation.RoleNotFoundException;
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

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Mono<ProblemDetail> handleUserAlreadyExistsException(UserAlreadyExistsException ex, ServerWebExchange exchange){
        return Mono.just(
                ProblemDetailsUtils.problemDetail(
                        HttpStatus.CONFLICT,
                        "User already exists",
                        ex.getMessage(),
                        exchange
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> badCredentialsExceptionHandler(BadCredentialsException ex, ServerWebExchange request){
        ProblemDetail body = ProblemDetailsUtils.problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ProblemDetail> roleNotFoundExceptionHandler(RoleNotFoundException ex, ServerWebExchange request){
        ProblemDetail body = ProblemDetailsUtils.problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ProblemDetail> usernameNotFoundExceptionHandler(UsernameNotFoundException ex, ServerWebExchange request){
        ProblemDetail body = ProblemDetailsUtils.problemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), "", request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
