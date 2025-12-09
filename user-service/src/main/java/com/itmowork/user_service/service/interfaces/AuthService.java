package com.itmowork.user_service.service.interfaces;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.AuthResponseDto;
import reactor.core.publisher.Mono;

public interface AuthService {

    Mono<AuthResponseDto> registerUser(UserRequestDto userRequestDto);
}
