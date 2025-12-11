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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
            "72eadf75ac2a262555bdda7b35a69c9c5b42f4f4d4299efc3ca272087892f1d5";

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
        return prepareVacancy(companyId, VacancyStatusName.DRAFT);
    }

    private Vacancy prepareVacancy(UUID companyId, VacancyStatusName statusName) {
        Currency currency = currencyService.findCurrencyById(1L);
        VacancyStatus status = vacancyStatusService.findByVacancyStatusName(statusName);

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
    @DisplayName("POST /api/vacancies/draft с валидным JWT и ролью COMPANY_OWNER -> 201 CREATED (DRAFT)")
    void createDraft_withValidJwt_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(companyClient.existsCompany(eq(companyId))).thenReturn(true);
        when(companyClient.validateCompanyOwnership(eq(companyId), eq(userId))).thenReturn(true);

        String token = generateJwt(userId, "owner@example.com", "ROLE_COMPANY_OWNER");

        VacancyStatus draftStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.DRAFT);

        Map<String, Object> body = Map.of(
                "title", "Draft vacancy",
                "description", "Draft description",
                "salary_from", 500,
                "salary_to", 1500,
                "company_id", companyId,
                "currency_id", 1L
        );

        mockMvc.perform(
                        post("/api/vacancies/draft")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Draft vacancy"))
                .andExpect(jsonPath("$.status_id").value(draftStatus.getId()));
    }

    @Test
    @DisplayName("POST /api/vacancies/draft без JWT -> 401 UNAUTHORIZED")
    void createDraft_withoutJwt_shouldReturn401() throws Exception {
        UUID companyId = UUID.randomUUID();

        Map<String, Object> body = Map.of(
                "title", "Draft vacancy",
                "description", "Draft description",
                "salary_from", 500,
                "salary_to", 1500,
                "company_id", companyId,
                "currency_id", 1L
        );

        mockMvc.perform(
                        post("/api/vacancies/draft")
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

    @Test
    @DisplayName("PATCH /api/vacancies/{id}/update с валидным JWT и ролью COMPANY_OWNER -> 200 OK")
    void updateVacancy_withValidJwt_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Vacancy vacancy = prepareVacancy(companyId);

        when(companyClient.existsCompany(eq(companyId))).thenReturn(true);
        when(companyClient.validateCompanyOwnership(eq(companyId), eq(userId))).thenReturn(true);

        String token = generateJwt(userId, "owner@example.com", "ROLE_COMPANY_OWNER");

        Map<String, Object> body = Map.of(
                "title", "Updated title",
                "description", "Updated description",
                "salary_from", 1500,
                "salary_to", 2500
        );

        mockMvc.perform(
                        patch("/api/vacancies/{id}/update", vacancy.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vacancy.getId().toString()))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.salary_from").value(1500))
                .andExpect(jsonPath("$.salary_to").value(2500));
    }

    @Test
    @DisplayName("PATCH /api/vacancies/{id}/update без JWT -> 401 UNAUTHORIZED")
    void updateVacancy_withoutJwt_shouldReturn401() throws Exception {
        UUID companyId = UUID.randomUUID();
        Vacancy vacancy = prepareVacancy(companyId);

        Map<String, Object> body = Map.of(
                "title", "Updated title"
        );

        mockMvc.perform(
                        patch("/api/vacancies/{id}/update", vacancy.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/vacancies/{id}/change-status DRAFT -> PUBLISHED с COMPANY_OWNER -> 200 OK")
    void changeStatus_withValidJwt_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Vacancy vacancy = prepareVacancy(companyId, VacancyStatusName.DRAFT);

        when(companyClient.existsCompany(eq(companyId))).thenReturn(true);
        when(companyClient.validateCompanyOwnership(eq(companyId), eq(userId))).thenReturn(true);

        VacancyStatus publishedStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.PUBLISHED);

        String token = generateJwt(userId, "owner@example.com", "ROLE_COMPANY_OWNER");

        mockMvc.perform(
                        patch("/api/vacancies/{id}/change-status", vacancy.getId())
                                .param("newStatus", VacancyStatusName.PUBLISHED.name())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vacancy.getId().toString()))
                .andExpect(jsonPath("$.status_id").value(publishedStatus.getId()));
    }

    @Test
    @DisplayName("PATCH /api/vacancies/{id}/change-status без JWT -> 401 UNAUTHORIZED")
    void changeStatus_withoutJwt_shouldReturn401() throws Exception {
        UUID companyId = UUID.randomUUID();
        Vacancy vacancy = prepareVacancy(companyId, VacancyStatusName.DRAFT);

        mockMvc.perform(
                        patch("/api/vacancies/{id}/change-status", vacancy.getId())
                                .param("newStatus", VacancyStatusName.PUBLISHED.name())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/vacancies/{id}/update-and-change-status с COMPANY_OWNER -> 200 OK")
    void updateAndChangeStatus_withValidJwt_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Vacancy vacancy = prepareVacancy(companyId, VacancyStatusName.DRAFT);

        when(companyClient.existsCompany(eq(companyId))).thenReturn(true);
        when(companyClient.validateCompanyOwnership(eq(companyId), eq(userId))).thenReturn(true);

        VacancyStatus publishedStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.PUBLISHED);

        String token = generateJwt(userId, "owner@example.com", "ROLE_COMPANY_OWNER");

        Map<String, Object> body = Map.of(
                "title", "Updated & published"
        );

        mockMvc.perform(
                        patch("/api/vacancies/{id}/update-and-change-status", vacancy.getId())
                                .param("newStatus", VacancyStatusName.PUBLISHED.name())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vacancy.getId().toString()))
                .andExpect(jsonPath("$.title").value("Updated & published"))
                .andExpect(jsonPath("$.status_id").value(publishedStatus.getId()));
    }

    @Test
    @DisplayName("PATCH /api/vacancies/{id}/update-and-change-status без JWT -> 401 UNAUTHORIZED")
    void updateAndChangeStatus_withoutJwt_shouldReturn401() throws Exception {
        UUID companyId = UUID.randomUUID();
        Vacancy vacancy = prepareVacancy(companyId, VacancyStatusName.DRAFT);

        Map<String, Object> body = Map.of(
                "title", "Updated & published"
        );

        mockMvc.perform(
                        patch("/api/vacancies/{id}/update-and-change-status", vacancy.getId())
                                .param("newStatus", VacancyStatusName.PUBLISHED.name())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/vacancies/{id}/title с ролью EMPLOYEE -> 200 OK")
    void getVacancyTitle_withEmployeeRole_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Vacancy vacancy = prepareVacancy(companyId);
        String expectedTitle = vacancy.getTitle();

        String token = generateJwt(userId, "employee@example.com", "ROLE_EMPLOYEE");

        mockMvc.perform(
                        get("/api/vacancies/{id}/title", vacancy.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedTitle));
    }

    @Test
    @DisplayName("GET /api/vacancies/{id}/title без JWT -> 401 UNAUTHORIZED")
    void getVacancyTitle_withoutJwt_shouldReturn401() throws Exception {
        UUID companyId = UUID.randomUUID();
        Vacancy vacancy = prepareVacancy(companyId);

        mockMvc.perform(
                        get("/api/vacancies/{id}/title", vacancy.getId())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllPublishedVacancies_shouldReturnOnlyPublished() throws Exception {
        UUID companyId = UUID.randomUUID();
        vacancyRepository.deleteAll();

        Vacancy published = prepareVacancy(companyId, VacancyStatusName.PUBLISHED);
        Vacancy draft = prepareVacancy(companyId, VacancyStatusName.DRAFT);

        mockMvc.perform(get("/api/vacancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(published.getId().toString()))
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(draft.getId().toString())
                )));
    }

    @Test
    void isVacancyPublished_publishedVacancy_shouldReturnTrue() throws Exception {
        UUID companyId = UUID.randomUUID();
        Vacancy vacancy = prepareVacancy(companyId, VacancyStatusName.PUBLISHED);

        mockMvc.perform(
                        get("/api/vacancies/{id}/is-published", vacancy.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void existsVacancy_existingVacancy_shouldReturnTrue() throws Exception {
        UUID companyId = UUID.randomUUID();
        Vacancy vacancy = prepareVacancy(companyId);

        mockMvc.perform(
                        get("/api/vacancies/{id}/exists", vacancy.getId())
                )
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void existsVacancy_nonExistingVacancy_shouldReturnFalse() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/vacancies/{id}/exists", randomId)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
