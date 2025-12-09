package org.itmowork.vacancy_service.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import org.itmowork.vacancy_service.service.interfaces.CurrencyService;
import org.itmowork.vacancy_service.service.interfaces.VacancyStatusService;
import org.springframework.transaction.annotation.Transactional;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.itmowork.vacancy_service.dto.request.VacancyCreateRequestDto;
import org.itmowork.vacancy_service.dto.request.VacancyUpdateRequestDto;
import org.itmowork.vacancy_service.model.Currency;
import org.itmowork.vacancy_service.model.Vacancy;
import org.itmowork.vacancy_service.model.VacancyStatus;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.itmowork.vacancy_service.repository.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 9999)
@TestPropertySource(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.openfeign.client.config.user-service.url=http://localhost:9999",
        "spring.cloud.openfeign.client.config.company-service.url=http://localhost:9999",
        "spring.cloud.openfeign.client.config.vacancy-service.url=http://localhost:9999"
})
class VacancyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VacancyRepository vacancyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VacancyStatusService vacancyStatusService;

    @Autowired
    private CurrencyService currencyService;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("vacancy-db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    @BeforeEach
    void setup() {
        WireMock.reset();
        vacancyRepository.deleteAll();
    }

    @Test
    void getCompanyIdByVacancyShouldReturnCompanyId() throws Exception {

        UUID companyId = UUID.randomUUID();

        Vacancy vacancy = Vacancy.builder()
                .title("Test vacancy")
                .description("Desc")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(Currency.builder().id(1L).currency("RUB").build())
                .status(VacancyStatus.builder()
                        .id(1L)
                        .vacancyStatusName(VacancyStatusName.DRAFT)
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        Vacancy saved = vacancyRepository.save(vacancy);
        UUID vacancyId = saved.getId();

        WireMock.stubFor(
                WireMock.get(urlEqualTo("/api/company/" + companyId))
                        .willReturn(okJson("true"))
        );

        mockMvc.perform(get("/api/vacancies/{id}/company-id", vacancyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(companyId.toString()));
    }

    @Test
    void createDraftVacancyShouldCreateVacancyWithDraftStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        VacancyCreateRequestDto request = VacancyCreateRequestDto.builder()
                .title("Draft vacancy")
                .description("Some description")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currencyId(1L)
                .build();

        WireMock.stubFor(
                WireMock.get(urlPathMatching("/api/users/.*"))
                        .willReturn(okJson("true"))
        );

        WireMock.stubFor(
                WireMock.get(urlPathMatching("/api/company/.*"))
                        .willReturn(okJson("true"))
        );

        mockMvc.perform(post("/api/vacancies/{userId}/draft", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Draft vacancy"))
                .andExpect(jsonPath("$.status_id").value(2));

        List<Vacancy> all = vacancyRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Draft vacancy");
        assertThat(all.get(0).getStatus().getId()).isEqualTo(2L);
    }

    @Test
    void createPublishedVacancyShouldCreateVacancyWithPublidshedStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        VacancyCreateRequestDto request = VacancyCreateRequestDto.builder()
                .title("Published vacancy")
                .description("Some description")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currencyId(1L)
                .build();

        WireMock.stubFor(
                WireMock.get(urlPathMatching("/api/users/.*"))
                        .willReturn(okJson("true"))
        );

        WireMock.stubFor(
                WireMock.get(urlPathMatching("/api/company/.*"))
                        .willReturn(okJson("true"))
        );

        mockMvc.perform(post("/api/vacancies/{userId}/publish", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Published vacancy"))
                .andExpect(jsonPath("$.status_id").value(1));

        List<Vacancy> all = vacancyRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getTitle()).isEqualTo("Published vacancy");
        assertThat(all.get(0).getStatus().getId()).isEqualTo(1L);
    }

    @Test
    @Transactional
    void changeStatusShouldChangeStatusFromDraftToPublished() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        VacancyStatus draftStatus = VacancyStatus.builder()
                .id(2L)
                .vacancyStatusName(VacancyStatusName.DRAFT)
                .build();

        Vacancy vacancy = Vacancy.builder()
                .title("Vacancy to change")
                .description("Some description")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(Currency.builder().id(1L).currency("RUB").build())
                .status(draftStatus)
                .createdAt(LocalDateTime.now())
                .build();

        Vacancy saved = vacancyRepository.save(vacancy);
        UUID vacancyId = saved.getId();

        WireMock.stubFor(
                WireMock.get(urlPathMatching("/api/company/.*"))
                        .willReturn(okJson("true"))
        );

        mockMvc.perform(patch("/api/vacancies/{userId}/{id}/change-status", userId, vacancyId)
                        .param("newStatus", VacancyStatusName.PUBLISHED.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vacancyId.toString()))
                .andExpect(jsonPath("$.status_id").isNumber());

        Vacancy updated = vacancyRepository.findById(vacancyId).orElseThrow();
        assertThat(updated.getStatus().getVacancyStatusName())
                .isEqualTo(VacancyStatusName.PUBLISHED);
    }

    @Test
    @Transactional
    void updateVacancyShouldUpdateFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        VacancyStatus draftStatus = VacancyStatus.builder()
                .id(2L)
                .vacancyStatusName(VacancyStatusName.DRAFT)
                .build();

        Currency rub = Currency.builder()
                .id(1L)
                .currency("RUB")
                .build();

        Vacancy vacancy = Vacancy.builder()
                .title("Old title")
                .description("Old description")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(rub)
                .status(draftStatus)
                .createdAt(LocalDateTime.now())
                .build();

        Vacancy saved = vacancyRepository.save(vacancy);
        UUID vacancyId = saved.getId();

        VacancyUpdateRequestDto dto = VacancyUpdateRequestDto.builder()
                .title("Updated title")
                .description("Updated description")
                .salaryFrom(150)
                .salaryTo(300)
                .build();

        WireMock.stubFor(
                WireMock.get(urlPathMatching("/api/company/.*"))
                        .willReturn(okJson("true"))
        );

        mockMvc.perform(patch("/api/vacancies/{userId}/{id}/update", userId, vacancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vacancyId.toString()))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.salary_from").value(150))
                .andExpect(jsonPath("$.salary_to").value(300));

        Vacancy updated = vacancyRepository.findById(vacancyId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated title");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getSalaryFrom()).isEqualTo(150);
        assertThat(updated.getSalaryTo()).isEqualTo(300);
        assertThat(updated.getStatus().getVacancyStatusName())
                .isEqualTo(VacancyStatusName.DRAFT);
    }

    @Test
    @Transactional
    void updateAndChangeStatusShouldUpdateFieldsAndChangeStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        VacancyStatus draftStatus = VacancyStatus.builder()
                .id(2L)
                .vacancyStatusName(VacancyStatusName.DRAFT)
                .build();

        Currency rub = Currency.builder()
                .id(1L)
                .currency("RUB")
                .build();

        Vacancy vacancy = Vacancy.builder()
                .title("Old title")
                .description("Old description")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(rub)
                .status(draftStatus)
                .createdAt(LocalDateTime.now())
                .build();

        Vacancy saved = vacancyRepository.save(vacancy);
        UUID vacancyId = saved.getId();

        VacancyUpdateRequestDto dto = VacancyUpdateRequestDto.builder()
                .title("Updated & published title")
                .description("Updated description")
                .salaryFrom(150)
                .salaryTo(300)
                .build();

        WireMock.stubFor(
                WireMock.get(urlPathMatching("/api/company/.*"))
                        .willReturn(okJson("true"))
        );

        mockMvc.perform(
                        patch("/api/vacancies/{userId}/{id}/update-and-change-status", userId, vacancyId)
                                .param("newStatus", VacancyStatusName.PUBLISHED.name())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vacancyId.toString()))
                .andExpect(jsonPath("$.title").value("Updated & published title"))
                .andExpect(jsonPath("$.salary_from").value(150))
                .andExpect(jsonPath("$.salary_to").value(300))
                .andExpect(jsonPath("$.status_id").isNumber());

        Vacancy updated = vacancyRepository.findById(vacancyId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated & published title");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getSalaryFrom()).isEqualTo(150);
        assertThat(updated.getSalaryTo()).isEqualTo(300);
        assertThat(updated.getStatus().getVacancyStatusName())
                .isEqualTo(VacancyStatusName.PUBLISHED);
    }

    @Test
    @Transactional
    void getAllPublishedVacanciesShouldReturnOnlyPublished() throws Exception {
        UUID companyId = UUID.randomUUID();

        VacancyStatus draftStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.DRAFT);
        VacancyStatus publishedStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.PUBLISHED);
        Currency rub = currencyService.findCurrencyById(1L);

        Vacancy draft = Vacancy.builder()
                .title("Draft vacancy")
                .description("Draft description")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(rub)
                .status(draftStatus)
                .createdAt(LocalDateTime.now())
                .build();

        Vacancy published = Vacancy.builder()
                .title("Published vacancy")
                .description("Published description")
                .salaryFrom(150)
                .salaryTo(300)
                .companyId(companyId)
                .currency(rub)
                .status(publishedStatus)
                .createdAt(LocalDateTime.now())
                .build();

        vacancyRepository.saveAll(List.of(draft, published));

        mockMvc.perform(get("/api/vacancies")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Published vacancy"));
    }

    @Test
    @Transactional
    void getVacancyTitleShouldReturnTitle() throws Exception {
        UUID companyId = UUID.randomUUID();

        VacancyStatus draftStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.DRAFT);
        Currency rub = currencyService.findCurrencyById(1L);

        Vacancy vacancy = Vacancy.builder()
                .title("Some vacancy title")
                .description("Desc")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(rub)
                .status(draftStatus)
                .createdAt(LocalDateTime.now())
                .build();

        vacancy = vacancyRepository.save(vacancy);

        mockMvc.perform(get("/api/vacancies/{id}/title", vacancy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Some vacancy title"));
    }

    @Test
    @Transactional
    void isVacancyPublishedShouldReturnTrueForPublished() throws Exception {
        UUID companyId = UUID.randomUUID();

        VacancyStatus publishedStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.PUBLISHED);
        Currency rub = currencyService.findCurrencyById(1L);

        Vacancy vacancy = Vacancy.builder()
                .title("Published vacancy")
                .description("Desc")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(rub)
                .status(publishedStatus)
                .createdAt(LocalDateTime.now())
                .build();

        vacancy = vacancyRepository.save(vacancy);

        mockMvc.perform(get("/api/vacancies/{id}/is-published", vacancy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @Transactional
    void isVacancyPublishedShouldReturnFalseForDraft() throws Exception {
        UUID companyId = UUID.randomUUID();

        VacancyStatus draftStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.DRAFT);
        Currency rub = currencyService.findCurrencyById(1L);

        Vacancy vacancy = Vacancy.builder()
                .title("Draft vacancy")
                .description("Desc")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(rub)
                .status(draftStatus)
                .createdAt(LocalDateTime.now())
                .build();

        vacancy = vacancyRepository.save(vacancy);

        mockMvc.perform(get("/api/vacancies/{id}/is-published", vacancy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    @Transactional
    void existsShouldReturnTrueIfVacancyExists() throws Exception {
        UUID companyId = UUID.randomUUID();

        VacancyStatus draftStatus =
                vacancyStatusService.findByVacancyStatusName(VacancyStatusName.DRAFT);
        Currency rub = currencyService.findCurrencyById(1L);

        Vacancy vacancy = Vacancy.builder()
                .title("Exists vacancy")
                .description("Desc")
                .salaryFrom(100)
                .salaryTo(200)
                .companyId(companyId)
                .currency(rub)
                .status(draftStatus)
                .createdAt(LocalDateTime.now())
                .build();

        vacancy = vacancyRepository.save(vacancy);

        mockMvc.perform(get("/api/vacancies/{id}/exists", vacancy.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void existsShouldReturnFalseForUnknownId() throws Exception {
        mockMvc.perform(get("/api/vacancies/{id}/exists", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}


