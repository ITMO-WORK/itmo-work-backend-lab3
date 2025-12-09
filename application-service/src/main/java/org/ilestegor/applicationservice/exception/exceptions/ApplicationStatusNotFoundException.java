package org.ilestegor.applicationservice.exception.exceptions;

public class ApplicationStatusNotFoundException extends RuntimeException{
    public ApplicationStatusNotFoundException() {
    }

    public ApplicationStatusNotFoundException(String message) {
        super(message);
    }
}
