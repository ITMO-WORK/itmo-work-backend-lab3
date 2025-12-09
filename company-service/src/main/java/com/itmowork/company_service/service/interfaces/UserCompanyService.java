package com.itmowork.company_service.service.interfaces;

import com.itmowork.company_service.model.UserCompany;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserCompanyService {

    Mono<UserCompany> saveUserCompany(UserCompany userCompany);

    Mono<Boolean> validateCompanyOwnership(UUID companyId, UUID userId);
}
