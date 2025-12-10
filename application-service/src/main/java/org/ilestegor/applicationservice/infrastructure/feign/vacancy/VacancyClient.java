package org.ilestegor.applicationservice.infrastructure.feign.vacancy;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "vacancy-service", path = "/api/vacancies")
public interface VacancyClient {

    @GetMapping("/{id}/title")
    String getVacancyTitle(@PathVariable UUID id, @RequestHeader(HttpHeaders.AUTHORIZATION) String token);

    @GetMapping("/{id}/exists")
    Boolean isVacancyExists(@PathVariable UUID id, @RequestHeader(HttpHeaders.AUTHORIZATION) String token);

    @GetMapping("/{id}/is-published")
    Boolean isVacancyPublished(@PathVariable UUID id, @RequestHeader(HttpHeaders.AUTHORIZATION) String token);

    @GetMapping("/{vacancyId}/company-id")
    UUID getCompanyIdByVacancy(@PathVariable UUID vacancyId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token);
}
