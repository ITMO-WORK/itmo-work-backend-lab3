package org.ilestegor.applicationservice.controller;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.ilestegor.applicationservice.dto.request.ApplicationCreateRequestDto;
import org.ilestegor.applicationservice.dto.request.ApplicationStatusUpdateRequestDto;
import org.ilestegor.applicationservice.dto.response.ApplicationCreateResponseDto;
import org.ilestegor.applicationservice.dto.response.ApplicationStatusUpdateResponseDto;
import org.ilestegor.applicationservice.model.Application;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import org.ilestegor.applicationservice.repository.ApplicationStatusRepository;
import org.ilestegor.applicationservice.repository.ApplicationRepository;
import org.ilestegor.applicationservice.security.JwtServiceImpl;
import org.ilestegor.applicationservice.security.interfaces.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@Testcontainers
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "spring.cloud.openfeign.client.config.user-service.url=http://localhost:${wiremock.server.port}",
        "spring.cloud.openfeign.client.config.vacancy-service.url=http://localhost:${wiremock.server.port}",
        "spring.cloud.openfeign.client.config.company-service.url=http://localhost:${wiremock.server.port}",

        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
public class ApplicationServiceControllerTest {
    private static final String TEST_JWT_SECRET =
            "VGhpcy1pcy1hLWxvbmcgc2VjcmV0IGtleSBmb3IgdGVzdHM=";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("application-db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);


        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);

        registry.add("jwt.secret",() -> TEST_JWT_SECRET);
    }

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll().block();
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApplicationStatusRepository applicationStatusRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JwtService jwtService;

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
    void createApplication_shouldReturn201AndPersistToDb() {
        UUID TEST_USER_ID = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + TEST_USER_ID))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(TEST_USER_ID))));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/title"))
                .willReturn(okJson("\"Java Developer\"")));

        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");

        String jwt = generateJwt(TEST_USER_ID, "test@mail.com", "ROLE_USER");

        var entity = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .build()
                )
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ApplicationCreateResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(entity).isNotNull();
        assertThat(entity.id()).isNotNull();
        assertThat(entity.status()).isEqualTo(ApplicationStatusName.NEW.getValue());
        assertThat(entity.coverLetter()).isEqualTo("cover letter");

        var saved = applicationRepository.findById(entity.id()).block();
        assertThat(saved).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(saved.getVacancyId()).isEqualTo(vacancyId);
    }

    @Test
    void createApplication_shouldReturn404_whenUserNotFound() {
        UUID TEST_USER_ID = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + TEST_USER_ID))
                .willReturn(aResponse().withStatus(404)));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));
        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("true")));
        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/title"))
                .willReturn(okJson("\"Java Developer\"")));

        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");


        String jwt = generateJwt(TEST_USER_ID, "test@mail.com", "ROLE_USER");

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .build()
                )
                .bodyValue(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void createApplication_shouldReturn404_whenVacancyNotFound() {
        UUID TEST_USER_ID = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + TEST_USER_ID))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(TEST_USER_ID))));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("false")));

        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");

        String jwt = generateJwt(TEST_USER_ID, "test@mail.com", "ROLE_USER");
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .build()
                )
                .bodyValue(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createApplication_shouldReturn400_whenApplicationAlreadyExists() {
        UUID TEST_USER_ID = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();


        stubFor(get(urlEqualTo("/api/user/" + TEST_USER_ID))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(TEST_USER_ID))));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/title"))
                .willReturn(okJson("\"Java Developer\"")));


        var newStatus = applicationStatusRepository
                .findByApplicationStatusName(ApplicationStatusName.NEW)
                .blockOptional()
                .orElseThrow(() -> new IllegalStateException("Status NEW not found in DB"));

        var existing = new Application();
        existing.setUserId(TEST_USER_ID);
        existing.setVacancyId(vacancyId);
        existing.setCoverLetter("already applied");
        existing.setStatus(newStatus.getId());

        applicationRepository.save(existing).block();

        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");

        String jwt = generateJwt(TEST_USER_ID, "test@mail.com", "ROLE_USER");

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .build()
                )
                .bodyValue(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        var all = applicationRepository.findAll().collectList().block();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(all.getFirst().getVacancyId()).isEqualTo(vacancyId);
    }

    @Test
    void createApplication_shouldReturn409_whenVacancyNotPublished() {
        UUID TEST_USER_ID = UUID.randomUUID();
        var vacancyId = UUID.randomUUID();


        stubFor(get(urlEqualTo("/api/user/" + TEST_USER_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "id": "%s",
                          "email": "john.doe@example.com",
                          "full_name": "John Doe"
                        }
                        """.formatted(TEST_USER_ID))));



        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("false")));

        var request = new ApplicationCreateRequestDto(
                "my awesome cover letter"
        );

        String jwt = generateJwt(TEST_USER_ID, "test@mail.com", "ROLE_USER");

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        var all = applicationRepository.findAll().collectList().block();
        assertThat(all).isEmpty();
    }


    @Test
    void updateApplication_shouldReturn200AndUpdateCoverLetter() {
        UUID TEST_USER_ID = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + TEST_USER_ID))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(TEST_USER_ID))));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/title"))
                .willReturn(okJson("\"Java Developer\"")));

        Application existing = new Application();
        existing.setUserId(TEST_USER_ID);
        existing.setVacancyId(vacancyId);
        existing.setCoverLetter("old cover letter");
        existing.setStatus(1L);

        existing = applicationRepository.save(existing).block();
        assertThat(existing).isNotNull();


        var request = new ApplicationCreateRequestDto("updated cover letter");
        String jwt = generateJwt(TEST_USER_ID, "test@mail.com", "ROLE_USER");

        var response = webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application/{vacancyId}")
                        .build(vacancyId)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()

                .expectStatus().isOk()
                .expectBody(ApplicationCreateResponseDto.class)
                .returnResult()
                .getResponseBody();


        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.coverLetter()).isEqualTo("updated cover letter");
        assertThat(response.status()).isEqualTo(ApplicationStatusName.NEW.getValue());


        var all = applicationRepository.findAll().collectList().block();
        assertThat(all).hasSize(1);
        var saved = all.getFirst();

        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(saved.getVacancyId()).isEqualTo(vacancyId);
        assertThat(saved.getCoverLetter()).isEqualTo("updated cover letter");
    }

    @Test
    void updateApplicationStatus_shouldReturn200AndUpdateStatusInDb() {
        UUID TEST_USER_ID = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + TEST_USER_ID))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(TEST_USER_ID))));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/company-id"))
                .willReturn(okJson("\"%s\"".formatted(companyId))));

        stubFor(get(urlEqualTo("/api/company/" + companyId + "/" + TEST_USER_ID))
                .willReturn(okJson("true")));

        Application application = Application.builder()
                .userId(TEST_USER_ID)
                .vacancyId(vacancyId)
                .coverLetter("old cover letter")
                .status(3L)
                .build();

        application = applicationRepository.save(application).block();
        assertThat(application).isNotNull();

        ApplicationStatusUpdateRequestDto request =
                new ApplicationStatusUpdateRequestDto(ApplicationStatusName.REJECTED);


        Application finalApplication = application;
        String jwt = generateJwt(TEST_USER_ID, "test@mail.com", "ROLE_ADMIN");
        var response = webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application/{applicationId}/status")
                        .build(finalApplication.getId())
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationStatusUpdateResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.status())
                .isEqualTo(ApplicationStatusName.REJECTED.getValue());

        var fromDb = applicationRepository.findById(application.getId()).block();
        assertThat(fromDb).isNotNull();
    }

    @Test
    void updateApplicationStatus_shouldReturn404_whenApplicationNotFound() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        ApplicationStatusUpdateRequestDto request =
                new ApplicationStatusUpdateRequestDto(ApplicationStatusName.REJECTED);
        String jwt = generateJwt(userId, "test@mail.com", "ROLE_MANAGER");
        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application/{applicationId}/status")
                        .build(applicationId)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isNotFound();
    }
}
