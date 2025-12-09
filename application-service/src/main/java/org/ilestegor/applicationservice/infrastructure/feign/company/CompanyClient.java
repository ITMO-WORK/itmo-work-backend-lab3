package org.ilestegor.applicationservice.infrastructure.feign.company;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service", path = "/api/company")
public interface CompanyClient {

    @GetMapping("/{companyId}/{userId}")
    Boolean isUserBelongsToCompany(@PathVariable UUID companyId, @PathVariable UUID userId);
}
