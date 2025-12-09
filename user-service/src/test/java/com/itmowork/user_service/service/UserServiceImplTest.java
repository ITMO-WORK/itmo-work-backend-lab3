package com.itmowork.user_service.service;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.UserResponseDto;
import com.itmowork.user_service.exception.exceptions.UserAlreadyExistsException;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User existingUser;
    private UserRequestDto userRequestDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        existingUser = User.builder()
                .id(userId)
                .fullName("Existing User")
                .email("existing@mail.com")
                .password("password")
                .build();

        userRequestDto = new UserRequestDto(
                "Arslan",
                "password123",
                "john@mail.com"
        );
    }


    @Test
    void createUserSuccessTest() {
        User savedUser = User.builder()
                .id(userId)
                .fullName("Arslan")
                .email("john@mail.com")
                .password("password123")
                .build();

        when(userRepository.findUserByEmail("john@mail.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        StepVerifier.create(userService.createUser(userRequestDto))
                .expectNextMatches(response ->
                        response.id().equals(userId) &&
                                response.fullName().equals("Arslan") &&
                                response.email().equals("john@mail.com")
                )
                .verifyComplete();

        verify(userRepository).findUserByEmail("john@mail.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserUserAlreadyExistsTest() {
        when(userRepository.findUserByEmail("john@mail.com"))
                .thenReturn(Optional.of(existingUser));

        StepVerifier.create(userService.createUser(userRequestDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof UserAlreadyExistsException;
                    assert error.getMessage().equals("Пользователь с таким email уже существует");
                })
                .verify();

        verify(userRepository).findUserByEmail("john@mail.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findUserByIdSuccessTest() {
        UUID userId = UUID.randomUUID();
        UserResponseDto userResponseDto = new UserResponseDto(userId, "Arslan", "john@mail.com");

        when(userRepository.findUserById(userId)).thenReturn(userResponseDto);

        StepVerifier.create(userService.findUserById(userId))
                .expectNextMatches(dto -> dto.id().equals(userId))
                .verifyComplete();
    }
}
