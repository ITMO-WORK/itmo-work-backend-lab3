package com.itmowork.company_service.controller;


import com.github.tomakehurst.wiremock.client.WireMock;
import com.itmowork.company_service.dto.request.CompanyRequestDto;
import com.itmowork.company_service.dto.request.CompanyUpdateRequestDto;
import com.itmowork.company_service.repository.CompanyRepository;
import com.itmowork.company_service.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.parameters.P;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import wiremock.com.fasterxml.jackson.core.JsonProcessingException;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;


@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.openfeign.client.config.user-service.url=http://localhost:${wiremock.server.port}",
        "jwt.secret=bXlzdXBlcnNlY3JldG15c3VwZXJzZWNyZXRteXN1cGVyc2VjcmV0"

})
public class CompanyControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private JwtService jwtService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);

        registry.add("spring.liquibase.enabled", () -> true);

    }

    @AfterEach
    void tearDown() {
        companyRepository.deleteAll().block();
    }

    @Autowired
    private WebTestClient client;

    @Test
    void createCompanyIntegrationTest() {
        WireMock.stubFor(
                WireMock.post("/api/auth/register-company-owner")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                            {
                                                "id": "11111111-1111-1111-1111-111111111111",
                                                "token": "mock-token"
                                            }
                                        """)
                        )
        );

        String jwt = generateJwt(
                USER_ID,
                "mock@user.com",
                "ROLE_COMPANY_OWNER"
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("MegaCorp")
                .jsonPath("$.user_id").isEqualTo(USER_ID.toString());

    }

    @Test
    void updateCompanyIntegrationTest() throws JsonProcessingException {
        WireMock.stubFor(
                WireMock.post("/api/auth/register-company-owner")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                    {
                                        "id": "11111111-1111-1111-1111-111111111111",
                                        "full_name": "Mock User",
                                        "email": "mock@user.com"
                                    }
                                    """)
                        )
        );

        String jwt = generateJwt(
                USER_ID,
                "mock@user.com",
                "ROLE_ADMIN"
        );


        CompanyRequestDto createReq = new CompanyRequestDto(
                "MegaCorp",
                "mc@example.com",
                "Desc",
                "Owner Name",
                "owner@example.com",
                "pass"
        );

        var createdCompany = client.post()
                .uri("/api/company/register-company")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .bodyValue(createReq)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .returnResult();

        byte[] responseBytes = createdCompany.getResponseBody();
        assert responseBytes != null;
        String json = new String(responseBytes, StandardCharsets.UTF_8);
        String companyId = new ObjectMapper()
                .readTree(json)
                .get("id")
                .asText();

        String userCreatedId = new ObjectMapper()
                .readTree(json)
                .get("user_id")
                .asText();

        var updateReq = new CompanyUpdateRequestDto(
                "MegaCorp UPDATED",
                "test@mail.com",
                "new description"
        );

        String jwtUpdate = generateJwt(
                userCreatedId.equals(USER_ID.toString()) ? USER_ID : UUID.fromString(userCreatedId),
                "mc@example.com",
                "ROLE_COMPANY_OWNER"
        );

        client.patch()
                .uri("/api/company/update-company/{id}",
                        companyId)
                .bodyValue(updateReq)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUpdate)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("MegaCorp UPDATED")
                .jsonPath("$.email").isEqualTo("test@mail.com")
                .jsonPath("$.description").isEqualTo("new description");
    }

    @Test
    void deleteCompanyIntegrationTest() throws JsonProcessingException {
        WireMock.stubFor(
                WireMock.post("/api/user/create")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                    {
                                        "id": "11111111-1111-1111-1111-111111111111",
                                        "full_name": "Mock User",
                                        "email": "mock@user.com"
                                    }
                                    """)
                        )
        );

        WireMock.stubFor(
                WireMock.get("/api/user/11111111-1111-1111-1111-111111111111")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                {
                                    "id": "11111111-1111-1111-1111-111111111111",
                                    "full_name": "Mock User",
                                    "email": "mock@user.com"
                                }
                                """)
                        )
        );

        CompanyRequestDto createReq = new CompanyRequestDto(
                "MegaCorp",
                "mc@example.com",
                "Desc",
                "Owner Name",
                "owner@example.com",
                "pass"
        );

        var created = client.post()
                .uri("/api/company/register-company")
                .bodyValue(createReq)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .returnResult();


        byte[] bytes = created.getResponseBody();
        assert bytes != null;
        String json = new String(bytes, StandardCharsets.UTF_8);
        String companyId = new ObjectMapper()
                .readTree(json)
                .get("id")
                .asText();

        WireMock.stubFor(
                WireMock.get("/api/user/11111111-1111-1111-1111-111111111111")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                    {
                                        "id": "11111111-1111-1111-1111-111111111111",
                                        "full_name": "Mock User",
                                        "email": "mock@user.com"
                                    }
                                    """)
                        )
        );
        client.delete()
                .uri("/api/company/delete/{id}/{userId}",
                        companyId,
                        "11111111-1111-1111-1111-111111111111")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(companyId)
                .jsonPath("$.message").isEqualTo("Компания была успешно удалена");
    }

    @Test
    void getAllCompaniesIntegrationTest() {
        WireMock.stubFor(
                WireMock.post("/api/user/create")
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("""
                                    {
                                        "id": "11111111-1111-1111-1111-111111111111",
                                        "full_name": "Mock User",
                                        "email": "mock@user.com"
                                    }
                                    """)
                        )
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

        client.post()
                .uri("/api/company/register-company")
                .bodyValue(req1)
                .exchange()
                .expectStatus().isCreated();

        client.post()
                .uri("/api/company/register-company")
                .bodyValue(req2)
                .exchange()
                .expectStatus().isCreated();

        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/company")
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].name").exists()
                .jsonPath("$.content[1].name").exists()
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.number").isEqualTo(0);
    }

    private ExchangeFilterFunction mockJwt(UUID userId) {
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
}
