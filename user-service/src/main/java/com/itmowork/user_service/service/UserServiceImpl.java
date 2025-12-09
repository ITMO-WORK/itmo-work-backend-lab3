package com.itmowork.user_service.service;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.UserResponseDto;
import com.itmowork.user_service.exception.exceptions.UserAlreadyExistsException;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import com.itmowork.user_service.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Mono<UserResponseDto> createUser(UserRequestDto userRequestDto) {
        return Mono.fromCallable(() ->{
            Optional<User> userOptional = userRepository.findUserByEmail(userRequestDto.email());
            if(userOptional.isPresent()) throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
            User user = User.builder()
                    .fullName(userRequestDto.fullName())
                    .password(userRequestDto.password())
                    .email(userRequestDto.email())
                    .build();
            User savedUser = userRepository.save(user);
            return mapToUserResponseDto(savedUser);
        })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<UserResponseDto> findUserById(UUID id) {
        return Mono.fromCallable(() -> userRepository.findUserById(id))
                .subscribeOn(Schedulers.boundedElastic());
    }


    private UserResponseDto mapToUserResponseDto(User user){
        return new UserResponseDto(
                user.getId(),
                user.getFullName(),
                user.getEmail()
        );
    }

}
