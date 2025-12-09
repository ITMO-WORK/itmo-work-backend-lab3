package com.itmowork.user_service.controller;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.UserResponseDto;
import com.itmowork.user_service.service.interfaces.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public Mono<ResponseEntity<UserResponseDto>> createUser(@RequestBody @Valid Mono<UserRequestDto> userRequestDto){
        return userRequestDto
                .flatMap(userService::createUser)
                .map(userResponseDto -> ResponseEntity.status(HttpStatus.CREATED).body(userResponseDto));

    }

    @GetMapping("/{id}")
    public Mono<UserResponseDto> findUserById(@PathVariable UUID id){
        return userService.findUserById(id);
    }
}
