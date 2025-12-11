package com.itmowork.user_service.controller;

import com.itmowork.user_service.dto.response.UserResponseDto;
import com.itmowork.user_service.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public Mono<UserResponseDto> findUserById(@PathVariable UUID id){
        return userService.findUserById(id);
    }
}
