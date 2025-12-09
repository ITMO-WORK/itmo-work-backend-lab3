package com.itmowork.user_service.controller;

import com.itmowork.user_service.dto.request.UserRequestDto;
import com.itmowork.user_service.model.User;
import com.itmowork.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
public class UserServiceControllerTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.liquibase.enabled", () -> true);

    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Autowired
    private WebTestClient client;

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

        client.get()
                .uri("/api/user/" + savedUser.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(savedUser.getId().toString())
                .jsonPath("$.full_name").isEqualTo("Arslan")
                .jsonPath("$.email").isEqualTo("john@example.com");
    }

}
