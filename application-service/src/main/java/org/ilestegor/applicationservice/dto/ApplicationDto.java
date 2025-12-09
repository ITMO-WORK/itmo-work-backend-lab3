package org.ilestegor.applicationservice.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record ApplicationDto (
        UUID id,
        String coverLetter,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID vacancyId,
        String vacancyTitle,
        UUID userId,
        String userFullName
){}
