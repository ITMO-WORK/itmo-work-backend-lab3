package com.itmowork.user_service.service;

import com.itmowork.user_service.dto.request.LoginRequestDto;
import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.AuthResponseDto;
import com.itmowork.user_service.exception.exceptions.UserAlreadyExistsException;
import com.itmowork.user_service.model.Role;
import com.itmowork.user_service.model.RoleName;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.security.JwtService;
import com.itmowork.user_service.service.interfaces.RoleService;
import com.itmowork.user_service.service.interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.management.relation.RoleNotFoundException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ReactiveAuthenticationManager reactiveAuthenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserRequestDto userRequest;
    private LoginRequestDto loginRequest;
    private UUID userId;
    private Role defaultRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userRequest = new UserRequestDto(
                "Arslan",
                "password123",
                "john@mail.com"
        );

        loginRequest = new LoginRequestDto(
                "password123",
                "john@mail.com"

        );

        defaultRole = Role.builder()
                .id(1L)
                .roleName(RoleName.ROLE_USER)
                .build();

        savedUser = User.builder()
                .id(userId)
                .fullName("Arslan")
                .email("john@mail.com")
                .password("encoded-pass")
                .role(List.of(defaultRole))
                .build();
    }


    @Nested
    class RegisterTests {

        @Test
        void registerUser_shouldCreateNewUserAndReturnTokens() {

            String rawPassword = userRequest.password();
            String encodedPassword = "encoded-pass";
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            when(userService.findUserByEmail("john@mail.com"))
                    .thenReturn(Mono.empty());

            when(roleService.findRoleByRoleName(RoleName.ROLE_USER))
                    .thenReturn(java.util.Optional.of(defaultRole));

            when(passwordEncoder.encode(rawPassword))
                    .thenReturn(encodedPassword);

            when(userService.saveUser(any(User.class)))
                    .thenReturn(Mono.just(savedUser));

            when(jwtService.generateAccessToken(any(Authentication.class), eq(userId), anyList()))
                    .thenReturn(accessToken);
            Mono<AuthResponseDto> result = authService.registerUser(userRequest);

            StepVerifier.create(result)
                    .assertNext(res -> {
                        assertEquals(accessToken, res.token());
                    })
                    .verifyComplete();

            verify(userService).findUserByEmail("john@mail.com");
            verify(roleService).findRoleByRoleName(RoleName.ROLE_USER);
            verify(passwordEncoder).encode(rawPassword);
            verify(userService).saveUser(any(User.class));
        }

        @Test
        void registerUser_shouldThrowUserAlreadyExists_whenEmailTaken() {

            when(userService.findUserByEmail("john@mail.com"))
                    .thenReturn(Mono.just(savedUser));

            Mono<AuthResponseDto> result = authService.registerUser(userRequest);

            StepVerifier.create(result)
                    .expectErrorSatisfies(error -> {
                        assertInstanceOf(UserAlreadyExistsException.class, error);
                        assertEquals("User already exists", error.getMessage());
                    })
                    .verify();

            verify(userService).findUserByEmail("john@mail.com");
            verify(roleService, never()).findRoleByRoleName(any());
            verify(userService, never()).saveUser(any());
        }

        @Test
        void registerUser_shouldThrowRoleNotFound_whenDefaultRoleMissing() {
            when(userService.findUserByEmail("john@mail.com"))
                    .thenReturn(Mono.empty());

            when(roleService.findRoleByRoleName(RoleName.ROLE_USER))
                    .thenReturn(java.util.Optional.empty());

            Mono<AuthResponseDto> result = authService.registerUser(userRequest);

            StepVerifier.create(result)
                    .expectErrorSatisfies(error -> {
                        assertTrue(error instanceof RoleNotFoundException);
                        assertTrue(error.getMessage().contains("Default role"));
                    })
                    .verify();

            verify(userService).findUserByEmail("john@mail.com");
            verify(roleService).findRoleByRoleName(RoleName.ROLE_USER);
            verify(userService, never()).saveUser(any());
        }
    }

    @Nested
    class LoginTests {

        @Test
        void loginUser_shouldAuthenticateAndReturnTokens() {
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            Authentication authResult = new UsernamePasswordAuthenticationToken(
                    loginRequest.email(),
                    loginRequest.password()
            );

            when(reactiveAuthenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(Mono.just(authResult));

            when(userService.findUserByEmail("john@mail.com"))
                    .thenReturn(Mono.just(savedUser));

            when(jwtService.generateAccessToken(any(Authentication.class), eq(userId), anyList()))
                    .thenReturn(accessToken);

            Mono<AuthResponseDto> result = authService.loginUser(loginRequest);

            StepVerifier.create(result)
                    .assertNext(res -> {
                        assertEquals(accessToken, res.token());
                    })
                    .verifyComplete();

            verify(reactiveAuthenticationManager)
                    .authenticate(any(Authentication.class));
            verify(userService).findUserByEmail("john@mail.com");
        }

        @Test
        void loginUser_shouldError_whenAuthFails() {
            when(reactiveAuthenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(Mono.error(new org.springframework.security.authentication.BadCredentialsException("Bad credentials")));

            Mono<AuthResponseDto> result = authService.loginUser(loginRequest);

            StepVerifier.create(result)
                    .expectError(org.springframework.security.authentication.BadCredentialsException.class)
                    .verify();

            verify(reactiveAuthenticationManager)
                    .authenticate(any(Authentication.class));
            verify(userService, never()).findUserByEmail(anyString());
        }
    }
}