package org.ilestegor.applicationservice.dto.request;

import org.ilestegor.applicationservice.model.ApplicationStatusName;

public record ApplicationStatusUpdateRequestDto(
        ApplicationStatusName applicationStatusName
){}
