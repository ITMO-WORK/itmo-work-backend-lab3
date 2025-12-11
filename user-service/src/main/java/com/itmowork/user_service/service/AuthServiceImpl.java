package com.itmowork.user_service.service;

import com.itmowork.user_service.dto.request.LoginRequestDto;
import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.AuthResponseDto;
import com.itmowork.user_service.dto.response.UserResponseDto;
import com.itmowork.user_service.exception.exceptions.UserAlreadyExistsException;
import com.itmowork.user_service.model.Role;
import com.itmowork.user_service.model.RoleName;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import com.itmowork.user_service.security.JwtService;
import com.itmowork.user_service.service.interfaces.AuthService;
import com.itmowork.user_service.service.interfaces.RoleService;
import com.itmowork.user_service.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.management.relation.RoleNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReactiveAuthenticationManager reactiveAuthenticationManager;
    private final RoleService roleService;

    @Override
    public Mono<AuthResponseDto> registerUser(UserRequestDto userRequestDto) {
        return registerUserByRoles(userRequestDto, RoleName.ROLE_USER);
    }

    @Override
    public Mono<AuthResponseDto> registerCompanyOwner(UserRequestDto userRequestDto) {
        return registerUserByRoles(userRequestDto, RoleName.ROLE_COMPANY_OWNER);
    }


    @Override
    public Mono<AuthResponseDto> loginUser(LoginRequestDto loginRequestDto) {
        String email = loginRequestDto.email();
        String password = loginRequestDto.password();

        Authentication authToken =
                new UsernamePasswordAuthenticationToken(email, password);

        return reactiveAuthenticationManager
                .authenticate(authToken)
                .flatMap(authentication ->
                        userService.findUserByEmail(email)
                                .switchIfEmpty(Mono.error(
                                        new BadCredentialsException("User not found")
                                ))
                )
                .flatMap(this::generateAuthResponse);
    }

    private Mono<AuthResponseDto> registerUserByRoles(UserRequestDto userRequestDto, RoleName roleName) {
        String email = userRequestDto.email();
        String rawPassword = userRequestDto.password();

        return userService.findUserByEmail(email)
                .flatMap(existingUser ->
                        Mono.<User>error(new UserAlreadyExistsException("User already exists"))
                )
                .switchIfEmpty(
                        Mono.defer(() ->
                                Mono.fromCallable(() ->
                                                roleService.findRoleByRoleName(roleName)
                                                        .orElseThrow(() -> new RoleNotFoundException(
                                                                "Default role " + roleName + " not found"
                                                        ))
                                        )
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMap(defaultRole -> {
                                            User newUser = User.builder()
                                                    .fullName(userRequestDto.fullName())
                                                    .email(email)
                                                    .password(passwordEncoder.encode(rawPassword))
                                                    .role(List.of(defaultRole))
                                                    .build();

                                            return userService.saveUser(newUser);
                                        })
                        )
                )
                .flatMap(this::generateAuthResponse);
    }

    private Mono<AuthResponseDto> generateAuthResponse(User user) {
        Authentication authentication = buildJwtAuthentication(user);
        String token = jwtService.generateAccessToken(authentication, user.getId(), authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        return Mono.just(new AuthResponseDto(user.getId(), token));
    }


    private Authentication buildJwtAuthentication(User user) {
        List<GrantedAuthority> authorities = buildAuthorities(user);
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                authorities
        );
    }

    private List<GrantedAuthority> buildAuthorities(User user) {
        return user.getRole().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName().name()))
                .collect(Collectors.toList());
    }
}
