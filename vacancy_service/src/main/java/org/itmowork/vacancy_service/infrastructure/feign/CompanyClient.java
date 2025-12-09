package org.itmowork.vacancy_service.infrastructure.feign;

import org.itmowork.vacancy_service.dto.response.CompanyResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "company-service", path = "/api/company")
public interface CompanyClient {

    @GetMapping("/{companyId}")
    Boolean existsCompany(@PathVariable("companyId") UUID companyId);

    @GetMapping("/{companyId}/{userId}")
    Boolean validateCompanyOwnership(
            @PathVariable UUID companyId,
            @PathVariable UUID userId
    );
}
