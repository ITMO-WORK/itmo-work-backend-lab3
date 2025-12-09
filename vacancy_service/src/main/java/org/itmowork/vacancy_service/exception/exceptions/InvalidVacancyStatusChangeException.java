package org.itmowork.vacancy_service.exception.exceptions;

public class InvalidVacancyStatusChangeException extends RuntimeException {
    public InvalidVacancyStatusChangeException(String message) {
        super(message);
    }
}