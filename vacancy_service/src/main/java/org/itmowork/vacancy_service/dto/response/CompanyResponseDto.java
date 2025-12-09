package org.itmowork.vacancy_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record CompanyResponseDto(
        UUID id,
        String name,
        String email,
        String description,
        String statusMessage,
        UUID userId
) {
}
