package org.ilestegor.applicationservice.contoller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ilestegor.applicationservice.dto.ApplicationDto;
import org.ilestegor.applicationservice.dto.request.ApplicationCreateRequestDto;
import org.ilestegor.applicationservice.dto.request.ApplicationStatusUpdateRequestDto;
import org.ilestegor.applicationservice.dto.response.ApplicationCreateResponseDto;
import org.ilestegor.applicationservice.dto.response.ApplicationStatusUpdateResponseDto;
import org.ilestegor.applicationservice.model.Application;
import org.ilestegor.applicationservice.service.interfaces.ApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public Mono<ResponseEntity<ApplicationCreateResponseDto>> createApplication(@RequestParam UUID vacancyId, @RequestParam UUID userId, @Valid @RequestBody ApplicationCreateRequestDto applicationCreateRequestDto){
        return applicationService.createApplication(vacancyId, userId, applicationCreateRequestDto).map(body -> new ResponseEntity<>(body, HttpStatus.CREATED));
    }

    @PatchMapping("/{vacancyId}")
    public Mono<ResponseEntity<ApplicationCreateResponseDto>> updateApplication(@PathVariable UUID vacancyId, @RequestParam UUID userId, @RequestBody ApplicationCreateRequestDto applicationCreateRequestDto){
        return applicationService.updateApplication(vacancyId, userId, applicationCreateRequestDto).map(body -> new ResponseEntity<>(body, HttpStatus.OK));
    }

    @PatchMapping("/{applicationId}/status")
    public Mono<ResponseEntity<ApplicationStatusUpdateResponseDto>> updateApplicationStatus(@PathVariable UUID applicationId, @RequestParam UUID userId, @RequestBody ApplicationStatusUpdateRequestDto applicationStatusUpdateRequestDto){
        return applicationService.updateApplicationStatus(applicationId, userId, applicationStatusUpdateRequestDto).map(body -> new ResponseEntity<>(body, HttpStatus.OK));
    }

    @GetMapping
    public Mono<Page<ApplicationDto>> getAllApplicationsByVacancyId(@RequestParam UUID vacancyId, @RequestParam UUID userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Pageable pageable = PageRequest.of(page, size);
        return applicationService.getAllApplicationsByVacancyId(vacancyId, userId, pageable);
    }
}
