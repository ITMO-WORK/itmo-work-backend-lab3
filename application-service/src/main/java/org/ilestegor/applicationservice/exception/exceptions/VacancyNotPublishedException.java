package org.ilestegor.applicationservice.exception.exceptions;

public class VacancyNotPublishedException extends RuntimeException{
    public VacancyNotPublishedException() {
    }

    public VacancyNotPublishedException(String message) {
        super(message);
    }
}
