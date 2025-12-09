package org.ilestegor.applicationservice.infrastructure.feign.vacancy;

import jakarta.ws.rs.Path;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "vacancy-service", path = "/api/vacancies")
public interface VacancyClient {

    @GetMapping("/{id}/title")
    String getVacancyTitle(@PathVariable UUID id);

    @GetMapping("/{id}/exists")
    Boolean isVacancyExists(@PathVariable UUID id);

    @GetMapping("/{id}/is-published")
    Boolean isVacancyPublished(@PathVariable UUID id);

    @GetMapping("/{vacancyId}/company-id")
    UUID getCompanyIdByVacancy(@PathVariable UUID vacancyId);
}
