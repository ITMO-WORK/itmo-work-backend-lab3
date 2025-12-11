package com.itmowork.user_service.controller;

import com.itmowork.user_service.dto.request.LoginRequestDto;
import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.model.Role;
import com.itmowork.user_service.model.RoleName;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import com.itmowork.user_service.security.JwtService;
import com.itmowork.user_service.service.interfaces.RoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@ExtendWith(SpringExtension.class)
class AuthControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.liquibase.enabled", () -> true);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleService roleService;

    @Autowired
    private JwtService jwtService;

    private String generateAdminJwt(UUID adminId, String email) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                authorities
        );

        return jwtService.generateAccessToken(
                auth,
                adminId,
                List.of("ROLE_ADMIN")
        );
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }


    @Test
    void registerUser_shouldCreateCompanyOwner_andReturnToken() {
        UUID adminId = UUID.randomUUID();
        String adminEmail = "admin@example.com";

        String adminJwt = generateAdminJwt(adminId, adminEmail);

        UserRequestDto request = new UserRequestDto(
                "Boss",
                "ownerPass",
                "owner@example.com"
        );

        webTestClient.post()
                .uri("/api/auth/register-company-owner")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.token").isNotEmpty();

        var savedOpt = userRepository.findUserByEmail("owner@example.com");
        assertThat(savedOpt).isPresent();

        User saved = savedOpt.get();
        assertThat(saved.getFullName()).isEqualTo("Boss");
        assertThat(passwordEncoder.matches("ownerPass", saved.getPassword()))
                .isTrue();

        assertThat(saved.getRole())
                .extracting(Role::getRoleName)
                .contains(RoleName.ROLE_COMPANY_OWNER);
    }

    @Test
    void registerUser_shouldReturnError_whenUserAlreadyExists() {
        Role userRole = roleService.findRoleByRoleName(RoleName.ROLE_USER)
                .orElseThrow();

        User existing = User.builder()
                .fullName("Existing")
                .email("john@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(List.of(userRole))
                .build();

        userRepository.save(existing);

        UserRequestDto request = new UserRequestDto(
                "Arslan",
                "password123",
                "john@example.com"
        );

        webTestClient.post()
                .uri("/api/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().is4xxClientError();

        assertThat(userRepository.findUserByEmail("john@example.com")).isPresent();
    }


    @Test
    void registerCompanyOwner_shouldCreateUserWithRoleCompanyOwner_andReturnToken() {
        UUID adminId = UUID.randomUUID();
        String adminEmail = "admin@example.com";

        String adminJwt = generateAdminJwt(adminId, adminEmail);

        UserRequestDto request = new UserRequestDto(
                "Boss",
                "ownerPass",
                "owner@example.com"
        );

        webTestClient.post()
                .uri("/api/auth/register-company-owner")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwt)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.token").isNotEmpty();

        var savedOpt = userRepository.findUserByEmail("owner@example.com");
        assertThat(savedOpt).isPresent();

        User saved = savedOpt.get();
        assertThat(saved.getFullName()).isEqualTo("Boss");
        assertThat(passwordEncoder.matches("ownerPass", saved.getPassword()))
                .isTrue();

        assertThat(saved.getRole())
                .extracting(Role::getRoleName)
                .contains(RoleName.ROLE_COMPANY_OWNER);
    }


    @Test
    void login_shouldAuthenticateAndReturnToken_whenCredentialsAreValid() {
        Role userRole = roleService.findRoleByRoleName(RoleName.ROLE_USER)
                .orElseThrow();

        String email = "john@example.com";
        String rawPassword = "password123";

        User user = User.builder()
                .fullName("Arslan")
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(List.of(userRole))
                .build();

        userRepository.save(user);

        LoginRequestDto loginRequest = new LoginRequestDto(
                rawPassword,
                email
        );

        webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(user.getId().toString())
                .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void login_shouldReturnError_whenPasswordIsInvalid() {
        Role userRole = roleService.findRoleByRoleName(RoleName.ROLE_USER)
                .orElseThrow();

        String email = "john@example.com";

        User user = User.builder()
                .fullName("Arslan")
                .email(email)
                .password(passwordEncoder.encode("correctPassword"))
                .role(List.of(userRole))
                .build();

        userRepository.save(user);

        LoginRequestDto loginRequest = new LoginRequestDto(
                email,
                "wrongPassword"
        );

        webTestClient.post()
                .uri("/api/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}