package com.itmowork.company_service.repository;

import com.itmowork.company_service.model.UserCompany;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserCompanyRepository extends ReactiveCrudRepository<UserCompany, Long> {

    Mono<Boolean> existsByCompanyIdAndUserId(UUID companyId, UUID userId);
}
