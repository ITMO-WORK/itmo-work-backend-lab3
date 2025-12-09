package com.itmowork.user_service.service;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.AuthResponseDto;
import com.itmowork.user_service.repository.UserRepository;
import com.itmowork.user_service.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    public Mono<AuthResponseDto> registerUser(UserRequestDto userRequestDto) {
        return null;
    }
}
