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

    Mono<ApplicationCreateResponseDto> createApplication(UUID vacancyId, ApplicationCreateRequestDto applicationCreateRequestDto);

    Mono<ApplicationCreateResponseDto> updateApplication(UUID vacancyId, ApplicationCreateRequestDto applicationCreateRequestDto);

    Mono<ApplicationStatusUpdateResponseDto> updateApplicationStatus(UUID applicationId, ApplicationStatusUpdateRequestDto applicationStatusUpdateRequestDto);

    Mono<Page<ApplicationDto>> getAllApplicationsByVacancyId(UUID vacancyId, Pageable pageable);
}
