package com.itmowork.company_service.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.itmowork.company_service.dto.request.CompanyRequestDto;
import com.itmowork.company_service.dto.request.CompanyUpdateRequestDto;
import com.itmowork.company_service.repository.CompanyRepository;
import com.itmowork.company_service.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.openfeign.client.config.user-service.url=http://localhost:${wiremock.server.port}",
        "jwt.secret=bXlzdXBlcnNlY3JldG15c3VwZXJzZWNyZXRteXN1cGVyc2VjcmV0"
})
public class CompanyControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WebTestClient baseClient;

    private WebTestClient client;

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" +
                        postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }

    @BeforeEach
    void setup() {
        String jwt = generateJwt(USER_ID, "mock@user.com", "ROLE_ADMIN", "ROLE_COMPANY_OWNER");

        this.client = baseClient
                .mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .filter(addAuthToken())
                .build();
    }

    @AfterEach
    void tearDown() {
        companyRepository.deleteAll().block();
    }

    @Test
    @Order(1)
    void createCompanyIntegrationTest() {
        WireMock.stubFor(
                WireMock.post("/api/auth/register-company-owner")
                        .willReturn(WireMock.okJson("""
                        {
                          "id": "11111111-1111-1111-1111-111111111111",
                          "token": "mock-token"
                        }
                        """))
        );

        CompanyRequestDto req = new CompanyRequestDto(
                "MegaCorp",
                "mc@example.com",
                "Desc",
                "Owner Name",
                "owner@example.com",
                "pass"
        );

        client.post()
                .uri("/api/company/register-company")
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("MegaCorp")
                .jsonPath("$.user_id").isEqualTo(USER_ID.toString());
    }

    @Test
    @Order(2)
    void updateCompanyIntegrationTest() throws Exception {
        WireMock.stubFor(
                WireMock.post("/api/auth/register-company-owner")
                        .willReturn(WireMock.okJson("""
                        {
                          "id": "11111111-1111-1111-1111-111111111111",
                          "token": "mock-token"
                        }
                        """))
        );

        CompanyRequestDto createReq = new CompanyRequestDto(
                "MegaCorp",
                "mc@example.com",
                "Desc",
                "Owner Name",
                "owner@example.com",
                "pass"
        );

        var createdResult = client.post()
                .uri("/api/company/register-company")
                .bodyValue(createReq)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        String createdJson = new String(createdResult.getResponseBody(), StandardCharsets.UTF_8);

        ObjectMapper om = new ObjectMapper();
        String companyId = om.readTree(createdJson).get("id").asText();

        CompanyUpdateRequestDto updateReq = new CompanyUpdateRequestDto(
                "MegaCorp UPDATED",
                "updated@mail.com",
                "new description"
        );

        client.patch()
                .uri("/api/company/update-company/{id}", companyId)
                .bodyValue(updateReq)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("MegaCorp UPDATED")
                .jsonPath("$.email").isEqualTo("updated@mail.com")
                .jsonPath("$.description").isEqualTo("new description");
    }

    @Test
    @Order(3)
    void deleteCompanyIntegrationTest() throws Exception {
        WireMock.stubFor(
                WireMock.post("/api/auth/register-company-owner")
                        .willReturn(WireMock.okJson("""
                        {
                          "id": "11111111-1111-1111-1111-111111111111",
                          "token": "mock-token"
                        }
                        """))
        );

        CompanyRequestDto req = new CompanyRequestDto(
                "MegaCorp",
                "mc@example.com",
                "Desc",
                "Owner Name",
                "owner@example.com",
                "pass"
        );

        var created = client.post()
                .uri("/api/company/register-company")
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult();

        String createdJson = new String(created.getResponseBody(), StandardCharsets.UTF_8);
        ObjectMapper om = new ObjectMapper();
        String companyId = om.readTree(createdJson).get("id").asText();

        client.delete()
                .uri("/api/company/delete/{id}", companyId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(companyId)
                .jsonPath("$.message").isEqualTo("Компания была успешно удалена");
    }

    @Test
    @Order(4)
    void getAllCompaniesIntegrationTest() {
        WireMock.stubFor(
                WireMock.post("/api/auth/register-company-owner")
                        .willReturn(WireMock.okJson("""
                        {
                          "id": "11111111-1111-1111-1111-111111111111",
                          "token": "mock-token"
                        }
                        """))
        );

        CompanyRequestDto req1 = new CompanyRequestDto(
                "MegaCorp",
                "mc@example.com",
                "Desc1",
                "Owner1",
                "owner1@example.com",
                "pass"
        );
        CompanyRequestDto req2 = new CompanyRequestDto(
                "SuperCorp",
                "sc@example.com",
                "Desc2",
                "Owner2",
                "owner2@example.com",
                "pass"
        );

        client.post().uri("/api/company/register-company").bodyValue(req1).exchange();
        client.post().uri("/api/company/register-company").bodyValue(req2).exchange();

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/company")
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.number").isEqualTo(0);
    }

    private ExchangeFilterFunction addAuthToken() {
        return ExchangeFilterFunction.ofRequestProcessor(req ->
                Mono.deferContextual(ctx ->
                        Mono.just(
                                ClientRequest.from(req)
                                        .attribute("authToken", "MOCK_TOKEN")
                                        .build()
                        )
                )
        );
    }

    private String generateJwt(UUID userId, String email, String... roles) {
        return jwtService.generateAccessToken(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        email, null,
                        java.util.Arrays.stream(roles)
                                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                .toList()
                ),
                userId,
                java.util.List.of(roles)
        );
    }
}