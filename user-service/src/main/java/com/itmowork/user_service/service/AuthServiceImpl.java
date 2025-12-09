package com.itmowork.user_service.service;

import com.itmowork.user_service.dto.request.LoginRequestDto;
import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.dto.response.AuthResponseDto;
import com.itmowork.user_service.exception.exceptions.UserAlreadyExistsException;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import com.itmowork.user_service.security.JwtService;
import com.itmowork.user_service.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.swing.text.html.Option;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public Mono<AuthResponseDto> registerUser(UserRequestDto userRequestDto) {
        String email = userRequestDto.email();
        String password = userRequestDto.password();

        return Mono.fromCallable(() ->
                        userRepository.findUserByEmail(email)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        return Mono.error(new UserAlreadyExistsException("User already exists"));
                    }

                    User user = User.builder()
                            .fullName(userRequestDto.fullName())
                            .email(email)
                            .password(passwordEncoder.encode(password))
                            .build();

                    return Mono.fromCallable(() ->
                            userRepository.save(user)
                    ).subscribeOn(Schedulers.boundedElastic());
                })
                .map(savedUser -> {

                    Authentication authentication =
                            new UsernamePasswordAuthenticationToken(
                                    savedUser.getEmail(),
                                    null,
                                    List.of()
                            );

                    String token = jwtService.generateAccessToken(authentication, savedUser.getId());

                    return new AuthResponseDto(savedUser.getId(), token);
                });
    }

    @Override
    public Mono<AuthResponseDto> loginUser(LoginRequestDto loginRequestDto) {
        String login = loginRequestDto.email();
        String password = loginRequestDto.password();

        return Mono.fromCallable(() -> userRepository.findUserByEmail(login)).subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isEmpty())
                        return Mono.error(new BadCredentialsException("Invalid email or password"));

                    User user = optionalUser.get();
                    if (!passwordEncoder.matches(password, user.getPassword()))
                        return Mono.error(new BadCredentialsException("Invalid password"));
                    Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.<SimpleGrantedAuthority>of());
                    String token = jwtService.generateAccessToken(authentication, user.getId());
                    AuthResponseDto authResponseDto = new AuthResponseDto(user.getId(), token);
                    return Mono.just(authResponseDto);
                });
    }
}
