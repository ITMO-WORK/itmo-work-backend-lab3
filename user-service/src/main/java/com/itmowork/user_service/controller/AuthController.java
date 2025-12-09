package com.itmowork.user_service.controller;

import com.itmowork.user_service.dto.request.LoginRequestDto;
import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.AuthResponseDto;
import com.itmowork.user_service.service.interfaces.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Mono<AuthResponseDto> register(@RequestBody @Valid Mono<UserRequestDto> userRequestDto){
        return userRequestDto
                .flatMap(authService::registerUser)
                .map(response -> response);
    }

    @PostMapping("/login")
    public Mono<AuthResponseDto> login(@RequestBody @Valid Mono<LoginRequestDto> loginRequestDto){
        return loginRequestDto.flatMap(authService::loginUser).map(resp -> resp);
    }
}
