package com.itmowork.company_service.service;

import com.itmowork.company_service.exception.exceptions.CompanyStatusNotFoundException;
import com.itmowork.company_service.model.CompanyStatus;
import com.itmowork.company_service.model.CompanyStatusName;
import com.itmowork.company_service.repository.CompanyStatusRepository;
import com.itmowork.company_service.service.interfaces.CompanyStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CompanyStatusServiceImpl implements CompanyStatusService {

    private final CompanyStatusRepository companyStatusRepository;

    @Override
    public Mono<CompanyStatus> findCompanyStatusByCompanyStatusName(CompanyStatusName companyStatusName) {
        return companyStatusRepository.findByStatus(companyStatusName)
                .switchIfEmpty(
                        Mono.error(new CompanyStatusNotFoundException("Компания с таким статусом не найдена"))
                );
    }
}
