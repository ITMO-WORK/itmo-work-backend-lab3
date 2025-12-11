package com.itmowork.user_service.service.interfaces;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.UserResponseDto;
import com.itmowork.user_service.model.User;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserService {

    Mono<UserResponseDto> createUser(UserRequestDto userRequestDto);
    Mono<UserResponseDto> findUserById(UUID id);
    Mono<User> findUserByEmail(String email);

    Mono<User> saveUser(User user);

}
