package com.itmowork.user_service.service;

import com.itmowork.user_service.dto.request.LoginRequestDto;
import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.AuthResponseDto;
import com.itmowork.user_service.exception.exceptions.UserAlreadyExistsException;
import com.itmowork.user_service.model.Role;
import com.itmowork.user_service.model.RoleName;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import com.itmowork.user_service.security.JwtService;
import com.itmowork.user_service.service.interfaces.AuthService;
import com.itmowork.user_service.service.interfaces.RoleService;
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

    //TODO change for user service later
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReactiveAuthenticationManager reactiveAuthenticationManager;
    private final RoleService roleService;

    @Override
    public Mono<AuthResponseDto> registerUser(UserRequestDto userRequestDto) {
        String email = userRequestDto.email();
        String rawPassword = userRequestDto.password();

        return Mono.fromCallable(() -> userRepository.findUserByEmail(email))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        return Mono.error(new UserAlreadyExistsException("User already exists"));
                    }

                    return Mono.fromCallable(() -> {

                        Role defaultRole = roleService.findRoleByRoleName(RoleName.ROLE_USER)
                                .orElseThrow(() -> new RoleNotFoundException("Default role ROLE_USER not found"));

                        User newUser = User.builder()
                                .fullName(userRequestDto.fullName())
                                .email(email)
                                .password(passwordEncoder.encode(rawPassword))
                                .role(List.of(defaultRole))
                                .build();

                        return userRepository.save(newUser);
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .flatMap(this::generateAuthResponse);
    }


    @Override
    public Mono<AuthResponseDto> loginUser(LoginRequestDto loginRequestDto) {
        String email = loginRequestDto.email();
        String password = loginRequestDto.password();

        Authentication authToken = new UsernamePasswordAuthenticationToken(email, password);

        return reactiveAuthenticationManager.authenticate(authToken)
                .flatMap(authentication -> Mono.fromCallable(() ->
                                userRepository.findUserByEmail(email)
                                        .orElseThrow(() -> new BadCredentialsException("User not found"))
                        ).subscribeOn(Schedulers.boundedElastic())
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
