package org.ilestegor.applicationservice.dto.response;

import java.time.LocalDateTime;

public record ApplicationStatusUpdateResponseDto (
        String status,
        LocalDateTime updateAt
){}
