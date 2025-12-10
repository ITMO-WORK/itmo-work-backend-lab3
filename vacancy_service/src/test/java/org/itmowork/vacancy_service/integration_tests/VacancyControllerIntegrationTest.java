package org.itmowork.vacancy_service.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.itmowork.vacancy_service.model.Currency;
import org.itmowork.vacancy_service.model.Vacancy;
import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.itmowork.vacancy_service.repository.VacancyRepository;
import org.itmowork.vacancy_service.infrastructure.feign.CompanyClient;
import org.itmowork.vacancy_service.service.interfaces.CurrencyService;
import org.itmowork.vacancy_service.service.interfaces.VacancyStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "spring.jackson.property-naming-strategy=SNAKE_CASE"
})
class VacancyControllerIntegrationTest {

    private static final String TEST_JWT_SECRET =
            "VGhpcy1pcy1hLWxvbmcgc2VjcmV0IGtleSBmb3IgdGVzdHM=";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("vacancy-db")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private VacancyStatusService vacancyStatusService;

    @MockitoBean
    private CompanyClient companyClient;

    private String generateJwt(UUID userId, String email, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_JWT_SECRET));

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("roles", List.of(roles))
                .signWith(key)
                .compact();
    }

    private Vacancy prepareVacancy(UUID companyId) {
        Currency currency = currencyService.findCurrencyById(1L);
        VacancyStatus status = vacancyStatusService.findByVacancyStatusName(VacancyStatusName.DRAFT);

        Vacancy vacancy = Vacancy.builder()
                .title("Test vacancy")
                .description("Some description")
                .salaryFrom(1000)
                .salaryTo(2000)
                .createdAt(LocalDateTime.now())
                .companyId(companyId)
                .currency(currency)
                .status(status)
                .build();

        return vacancyRepository.save(vacancy);
    }


    @Test
    @DisplayName("POST /api/vacancies/publish с валидным JWT и ролью COMPANY_OWNER -> 201 CREATED")
    void createPublish_withValidJwt_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(companyClient.existsCompany(eq(companyId))).thenReturn(true);
        when(companyClient.validateCompanyOwnership(eq(companyId), eq(userId))).thenReturn(true);

        String token = generateJwt(userId, "owner@example.com", "ROLE_COMPANY_OWNER");

        Map<String, Object> body = Map.of(
                "title", "Java Developer",
                "description", "Awesome vacancy",
                "salary_from", 1000,
                "salary_to", 2000,
                "company_id", companyId,
                "currency_id", 1L
        );

        mockMvc.perform(
                        post("/api/vacancies/publish")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Java Developer"))
                .andExpect(jsonPath("$.company_id").value(companyId.toString()));
    }

    @Test
    @DisplayName("POST /api/vacancies/publish без JWT -> 401 UNUATHORIZED (нет доступа)")
    void createPublish_withoutJwt_shouldReturn401() throws Exception {
        UUID companyId = UUID.randomUUID();

        Map<String, Object> body = Map.of(
                "title", "Java Developer",
                "description", "Awesome vacancy",
                "salary_from", 1000,
                "salary_to", 2000,
                "company_id", companyId,
                "currency_id", 1L
        );

        mockMvc.perform(
                        post("/api/vacancies/publish")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/vacancies/{id}/company-id с ролью EMPLOYEE -> 200 OK")
    void getCompanyId_withEmployeeRole_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        // заранее создаём вакансию в БД
        Vacancy vacancy = prepareVacancy(companyId);

        String token = generateJwt(userId, "employee@example.com", "ROLE_EMPLOYEE");

        mockMvc.perform(
                        get("/api/vacancies/{id}/company-id", vacancy.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + companyId.toString() + "\""));
    }

    @Test
    @DisplayName("GET /api/vacancies/{id}/company-id без нужной роли -> 403 FORBIDDEN")
    void getCompanyId_withWrongRole_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Vacancy vacancy = prepareVacancy(companyId);

        String token = generateJwt(userId, "user@example.com", "ROLE_USER");

        mockMvc.perform(
                        get("/api/vacancies/{id}/company-id", vacancy.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isForbidden());
    }
}
