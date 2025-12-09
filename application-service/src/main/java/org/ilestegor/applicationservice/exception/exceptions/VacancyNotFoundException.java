package org.ilestegor.applicationservice.exception.exceptions;

public class VacancyNotFoundException extends RuntimeException{
    public VacancyNotFoundException() {
    }

    public VacancyNotFoundException(String message) {
        super(message);
    }
}
