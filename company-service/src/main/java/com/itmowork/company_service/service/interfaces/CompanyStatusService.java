package com.itmowork.company_service.service.interfaces;

import com.itmowork.company_service.model.CompanyStatus;
import com.itmowork.company_service.model.CompanyStatusName;
import reactor.core.publisher.Mono;

public interface CompanyStatusService {

    Mono<CompanyStatus> findCompanyStatusByCompanyStatusName(CompanyStatusName companyStatusName);
}
