package com.itmowork.user_service.controller;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import com.itmowork.user_service.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@ExtendWith(SpringExtension.class)
public class UserServiceControllerTest {

    private static final String TEST_JWT_SECRET =
            "VGhpcy1pcy1hLWxvbmcgc2VjcmV0IGtleSBmb3IgdGVzdHM=";

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("user-db")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebTestClient client;

    @Autowired
    private JwtService jwtService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.liquibase.enabled", () -> true);

        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private String generateJwt(UUID userId, String email, String... roles) {
        var authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                authorities
        );

        return jwtService.generateAccessToken(
                auth,
                userId,
                List.of(roles)
        );
    }

    @Test
    void createUserSuccessIntegrationTest() {
        UserRequestDto request = new UserRequestDto(
                "Arslan",
                "password123",
                "john@example.com"
        );

        client.post()
                .uri("/api/user/create")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.full_name").isEqualTo("Arslan")
                .jsonPath("$.email").isEqualTo("john@example.com");

        assertThat(userRepository.findUserByEmail("john@example.com")).isPresent();
    }

    @Test
    void findUserByIdIntegrationTest() {
        User user = User.builder()
                .fullName("Arslan")
                .email("john@example.com")
                .password("12345")
                .build();

        User savedUser = userRepository.save(user);

        String jwt = generateJwt(savedUser.getId(), savedUser.getEmail(), "ROLE_USER");

        client.get()
                .uri("/api/user/" + savedUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(savedUser.getId().toString())
                .jsonPath("$.full_name").isEqualTo("Arslan")
                .jsonPath("$.email").isEqualTo("john@example.com");
    }
}