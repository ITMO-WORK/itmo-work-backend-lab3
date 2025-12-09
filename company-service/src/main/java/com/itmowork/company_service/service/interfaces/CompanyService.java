package com.itmowork.company_service.service.interfaces;

import com.itmowork.company_service.dto.request.CompanyRequestDto;
import com.itmowork.company_service.dto.request.CompanyUpdateRequestDto;
import com.itmowork.company_service.dto.response.CompanyDeleteResponseDto;
import com.itmowork.company_service.dto.response.CompanyResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CompanyService {

    Mono<CompanyResponseDto> createCompany(CompanyRequestDto companyRequestDto);

    Mono<CompanyResponseDto> updateCompany(UUID id, UUID userId, CompanyUpdateRequestDto companyUpdateRequestDto);

    Mono<CompanyDeleteResponseDto> deleteCompany(UUID id, UUID userId);

    Mono<Page<CompanyResponseDto>> getAllCompanies(Pageable pageable);

    Mono<Boolean> existsCompanyById(UUID id);
}
