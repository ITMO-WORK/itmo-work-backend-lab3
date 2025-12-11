package com.itmowork.company_service.controller;

import com.itmowork.company_service.dto.request.CompanyRequestDto;
import com.itmowork.company_service.dto.request.CompanyUpdateRequestDto;
import com.itmowork.company_service.dto.response.CompanyDeleteResponseDto;
import com.itmowork.company_service.dto.response.CompanyResponseDto;
import com.itmowork.company_service.service.interfaces.CompanyService;
import com.itmowork.company_service.service.interfaces.UserCompanyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final UserCompanyService userCompanyService;

    @PostMapping("/register-company")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<CompanyResponseDto>> createCompany(@RequestBody @Valid Mono<CompanyRequestDto> companyRequestDto){
        return companyRequestDto
                .flatMap(companyService::createCompany)
                .map(companyResponseDto ->
                        ResponseEntity.status(HttpStatus.CREATED).body(companyResponseDto));
    }

    @PatchMapping("/update-company/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<CompanyResponseDto>> updateCompany(@PathVariable UUID id, @RequestBody @Valid Mono<CompanyUpdateRequestDto> companyUpdateRequestDto){
        return companyUpdateRequestDto
                .flatMap(requestDto -> companyService.updateCompany(id, requestDto))
                .map(companyResponseDto ->
                        ResponseEntity.status(HttpStatus.OK).body(companyResponseDto));
    }

    @DeleteMapping("/delete/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<CompanyDeleteResponseDto>> deleteCompany(@PathVariable UUID id){
        return Mono.from(companyService.deleteCompany(id)
                .map(companyDeleteResponseDto ->
                        ResponseEntity.status(HttpStatus.OK).body(companyDeleteResponseDto)));
    }

    @GetMapping
    public Mono<Page<CompanyResponseDto>> getAllCompanies(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return companyService.getAllCompanies(pageable);

    }
    @GetMapping("/{companyId}/{userId}")
    public Mono<Boolean> validateCompanyOwnership(@PathVariable UUID companyId, @PathVariable UUID userId){
        return userCompanyService.validateCompanyOwnership(companyId, userId);
    }

    @GetMapping("/{id}")
    public Mono<Boolean> existsCompanyById(@PathVariable UUID id){
        return companyService.existsCompanyById(id);
    }
}
