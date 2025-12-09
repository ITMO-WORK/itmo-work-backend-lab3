package com.itmowork.company_service.repository;

import com.itmowork.company_service.model.CompanyStatus;
import com.itmowork.company_service.model.CompanyStatusName;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyStatusRepository extends ReactiveCrudRepository<CompanyStatus, Long> {

    Mono<CompanyStatus> findByStatus(CompanyStatusName companyStatusName);
}
