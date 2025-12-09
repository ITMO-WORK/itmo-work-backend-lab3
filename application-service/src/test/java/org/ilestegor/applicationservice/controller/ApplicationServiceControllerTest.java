package org.ilestegor.applicationservice.controller;


import org.ilestegor.applicationservice.dto.request.ApplicationCreateRequestDto;
import org.ilestegor.applicationservice.dto.request.ApplicationStatusUpdateRequestDto;
import org.ilestegor.applicationservice.dto.response.ApplicationCreateResponseDto;
import org.ilestegor.applicationservice.dto.response.ApplicationStatusUpdateResponseDto;
import org.ilestegor.applicationservice.model.Application;
import org.ilestegor.applicationservice.model.ApplicationStatusName;
import org.ilestegor.applicationservice.repository.ApplicationStatusRepository;
import org.ilestegor.applicationservice.repository.ApplicationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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


    @Test
    void createApplication_shouldReturn201AndPersistToDb() {

        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + userId))
                .willReturn(okJson("""
                    {
                      "id": "%s",
                      "full_name": "Test User",
                      "email": "test@mail.com"
                    }
                    """.formatted(userId))));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/title"))
                .willReturn(okJson("\"Java Developer\"")));


        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");

        var entity = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .queryParam("userId", userId)
                        .build()
                )
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
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getVacancyId()).isEqualTo(vacancyId);
    }

    @Test
    void createApplication_shouldReturn404_whenUserNotFound() {

        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + userId))
                .willReturn(aResponse().withStatus(404)));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));
        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("true")));
        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/title"))
                .willReturn(okJson("\"Java Developer\"")));

        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");


        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .queryParam("userId", userId)
                        .build()
                )
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void createApplication_shouldReturn404_whenVacancyNotFound() {
        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + userId))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(userId))));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("false")));

        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");


        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .queryParam("userId", userId)
                        .build()
                )
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createApplication_shouldReturn400_whenApplicationAlreadyExists() {

        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();


        stubFor(get(urlEqualTo("/api/user/" + userId))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(userId))));


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
        existing.setUserId(userId);
        existing.setVacancyId(vacancyId);
        existing.setCoverLetter("already applied");
        existing.setStatus(newStatus.getId());

        applicationRepository.save(existing).block();

        ApplicationCreateRequestDto request = new ApplicationCreateRequestDto("cover letter");

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .queryParam("userId", userId)
                        .build()
                )
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        var all = applicationRepository.findAll().collectList().block();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getUserId()).isEqualTo(userId);
        assertThat(all.getFirst().getVacancyId()).isEqualTo(vacancyId);
    }

    @Test
    void createApplication_shouldReturn409_whenVacancyNotPublished() {

        var userId = UUID.randomUUID();
        var vacancyId = UUID.randomUUID();


        stubFor(get(urlEqualTo("/api/user/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "id": "%s",
                          "email": "john.doe@example.com",
                          "full_name": "John Doe"
                        }
                        """.formatted(userId))));



        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("false")));

        var request = new ApplicationCreateRequestDto(
                "my awesome cover letter"
        );

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application")
                        .queryParam("vacancyId", vacancyId)
                        .queryParam("userId", userId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);


        var all = applicationRepository.findAll().collectList().block();
        assertThat(all).isEmpty();
    }


    @Test
    void updateApplication_shouldReturn200AndUpdateCoverLetter() {

        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + userId))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(userId))));


        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/is-published"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/title"))
                .willReturn(okJson("\"Java Developer\"")));

        Application existing = new Application();
        existing.setUserId(userId);
        existing.setVacancyId(vacancyId);
        existing.setCoverLetter("old cover letter");
        existing.setStatus(1L);

        existing = applicationRepository.save(existing).block();
        assertThat(existing).isNotNull();


        var request = new ApplicationCreateRequestDto("updated cover letter");

        var response = webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application/{vacancyId}")
                        .queryParam("userId", userId)
                        .build(vacancyId)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
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
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getVacancyId()).isEqualTo(vacancyId);
        assertThat(saved.getCoverLetter()).isEqualTo("updated cover letter");
    }

    @Test
    void updateApplicationStatus_shouldReturn200AndUpdateStatusInDb() {

        UUID userId = UUID.randomUUID();
        UUID vacancyId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        stubFor(get(urlEqualTo("/api/user/" + userId))
                .willReturn(okJson("""
                {
                  "id": "%s",
                  "full_name": "Test User",
                  "email": "test@mail.com"
                }
                """.formatted(userId))));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/exists"))
                .willReturn(okJson("true")));

        stubFor(get(urlEqualTo("/api/vacancies/" + vacancyId + "/company-id"))
                .willReturn(okJson("\"%s\"".formatted(companyId))));

        stubFor(get(urlEqualTo("/api/company/" + companyId + "/" + userId))
                .willReturn(okJson("true")));

        Application application = Application.builder()
                .userId(userId)
                .vacancyId(vacancyId)
                .coverLetter("old cover letter")
                .status(3L)
                .build();

        application = applicationRepository.save(application).block();
        assertThat(application).isNotNull();

        ApplicationStatusUpdateRequestDto request =
                new ApplicationStatusUpdateRequestDto(ApplicationStatusName.REJECTED);


        Application finalApplication = application;
        var response = webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application/{applicationId}/status")
                        .queryParam("userId", userId)
                        .build(finalApplication.getId())
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
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

        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/application/{applicationId}/status")
                        .queryParam("userId", userId)
                        .build(applicationId)
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }
}
