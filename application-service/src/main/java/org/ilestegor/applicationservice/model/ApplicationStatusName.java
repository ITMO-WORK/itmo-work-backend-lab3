package org.ilestegor.applicationservice.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationStatusName {
    NEW("new"),
    VIEWED("viewed"),
    REJECTED("rejected"),
    ACCEPTED("accepted");

    private final String value;

    ApplicationStatusName(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

