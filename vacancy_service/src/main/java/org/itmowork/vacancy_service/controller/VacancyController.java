package org.itmowork.vacancy_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.itmowork.vacancy_service.dto.request.VacancyCreateRequestDto;
import org.itmowork.vacancy_service.dto.request.VacancyUpdateRequestDto;
import org.itmowork.vacancy_service.dto.response.VacancyResponseDto;
import org.itmowork.vacancy_service.model.VacancyStatusName;
import org.itmowork.vacancy_service.service.interfaces.VacancyService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vacancies")
@RequiredArgsConstructor
public class VacancyController {

    private final VacancyService vacancyService;

    @PatchMapping("/{id}/update-and-change-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COMPANY_OWNER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VacancyResponseDto> updateAndChangeStatus(
            @PathVariable UUID id,
            @RequestBody @Valid VacancyUpdateRequestDto dto,
            @RequestParam VacancyStatusName newStatus
    ) {
        return ResponseEntity.ok(
                vacancyService.updateAndChangeStatus(id, dto, newStatus)
        );
    }

    @PatchMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COMPANY_OWNER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VacancyResponseDto> updateVacancy(
            @PathVariable UUID id,
            @RequestBody @Valid VacancyUpdateRequestDto dto
    ) {
        return ResponseEntity.ok(
                vacancyService.updateVacancy(id, dto)
        );
    }

    @PatchMapping("/{id}/change-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COMPANY_OWNER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VacancyResponseDto> changeStatus(
            @PathVariable UUID id,
            @RequestParam VacancyStatusName newStatus
    ) {
        return ResponseEntity.ok(
                vacancyService.changeStatus(id, newStatus)
        );
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COMPANY_OWNER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VacancyResponseDto> createPublish(
            @RequestBody @Valid VacancyCreateRequestDto request
    ) {
        VacancyResponseDto response =
                vacancyService.createVacancy(request, VacancyStatusName.PUBLISHED);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COMPANY_OWNER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VacancyResponseDto> createDraft(
            @RequestBody @Valid VacancyCreateRequestDto request
    ) {
        VacancyResponseDto response =
                vacancyService.createVacancy(request, VacancyStatusName.DRAFT);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PagedModel<VacancyResponseDto> getAllPublishedVacancies(Pageable pageable) {
        return new PagedModel<>(
                vacancyService.getAllPublishedVacancies(pageable)
        );
    }

    @GetMapping("/{vacancyId}/company-id")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<UUID> getCompanyIdByVacancy(@PathVariable UUID vacancyId) {
        UUID companyId = vacancyService.findCompanyIdByVacancyId(vacancyId);
        return ResponseEntity.ok(companyId);
    }

    @GetMapping("/{id}/title")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'COMPANY_OWNER', 'EMPLOYEE')")
    public ResponseEntity<String> getVacancyTitle(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyService.getVacancyTitle(id));
    }

    @GetMapping("/{id}/is-published")
    public ResponseEntity<Boolean> isVacancyPublished(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyService.isVacancyPublished(id));
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable UUID id) {
        return ResponseEntity.ok(vacancyService.existsVacancyById(id));
    }
}

