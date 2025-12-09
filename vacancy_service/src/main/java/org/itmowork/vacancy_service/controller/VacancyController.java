package org.itmowork.vacancy_service.controller;

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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vacancies")
@RequiredArgsConstructor
public class VacancyController {

    private final VacancyService vacancyService;

    @GetMapping("/{vacancyId}/company-id")
    public ResponseEntity<UUID> getCompanyIdByVacancy(@PathVariable UUID vacancyId) {
        UUID companyId = vacancyService.findCompanyIdByVacancyId(vacancyId);
        return ResponseEntity.ok(companyId);
    }

    @PostMapping("/{userId}/draft")
    public ResponseEntity<VacancyResponseDto> createDraftVacancy(
            @PathVariable UUID userId,
            @RequestBody @Valid VacancyCreateRequestDto request) {

        VacancyResponseDto response = vacancyService.createVacancy(
                userId,
                request,
                VacancyStatusName.DRAFT
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{userId}/publish")
    public ResponseEntity<VacancyResponseDto> createPublishedVacancy(
            @PathVariable UUID userId,
            @RequestBody @Valid VacancyCreateRequestDto request) {

        VacancyResponseDto response = vacancyService.createVacancy(
                userId,
                request,
                VacancyStatusName.PUBLISHED
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{userId}/{id}/change-status")
    public ResponseEntity<VacancyResponseDto> changeStatus(
            @PathVariable UUID userId,
            @PathVariable UUID id,
            @RequestParam VacancyStatusName newStatus) {

        return ResponseEntity.ok(
                vacancyService.changeStatus(userId, id, newStatus)
        );
    }

    @PatchMapping("/{userId}/{id}/update")
    public ResponseEntity<VacancyResponseDto> updateVacancy(
            @PathVariable UUID userId,
            @PathVariable UUID id,
            @RequestBody @Valid VacancyUpdateRequestDto dto) {

        return ResponseEntity.ok(
                vacancyService.updateVacancy(userId, id, dto)
        );
    }

    @PatchMapping("/{userId}/{id}/update-and-change-status")
    public ResponseEntity<VacancyResponseDto> updateAndChangeStatus(
            @PathVariable UUID userId,
            @PathVariable UUID id,
            @RequestBody @Valid VacancyUpdateRequestDto dto,
            @RequestParam VacancyStatusName newStatus) {

        return ResponseEntity.ok(
                vacancyService.updateAndChangeStatus(userId, id, dto, newStatus)
        );
    }

    @GetMapping
    public PagedModel<VacancyResponseDto> getAllPublishedVacancies(Pageable pageable) {
        return new PagedModel<>(
                vacancyService.getAllPublishedVacancies(pageable)
        );
    }

    @GetMapping("/{id}/title")
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

