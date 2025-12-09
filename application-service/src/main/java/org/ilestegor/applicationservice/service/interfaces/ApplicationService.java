package org.ilestegor.applicationservice.service.interfaces;

import org.ilestegor.applicationservice.dto.ApplicationDto;
import org.ilestegor.applicationservice.dto.request.ApplicationCreateRequestDto;
import org.ilestegor.applicationservice.dto.request.ApplicationStatusUpdateRequestDto;
import org.ilestegor.applicationservice.dto.response.ApplicationCreateResponseDto;
import org.ilestegor.applicationservice.dto.response.ApplicationStatusUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationService {

    Mono<ApplicationCreateResponseDto> createApplication(UUID id, UUID userId, ApplicationCreateRequestDto applicationCreateRequestDto);

    Mono<ApplicationCreateResponseDto> updateApplication(UUID id, UUID userId, ApplicationCreateRequestDto applicationCreateRequestDto);

    Mono<ApplicationStatusUpdateResponseDto> updateApplicationStatus(UUID applicationId, UUID userId, ApplicationStatusUpdateRequestDto applicationStatusUpdateRequestDto);

    Mono<Page<ApplicationDto>> getAllApplicationsByVacancyId(UUID vacancyId, UUID userId, Pageable pageable);
}
