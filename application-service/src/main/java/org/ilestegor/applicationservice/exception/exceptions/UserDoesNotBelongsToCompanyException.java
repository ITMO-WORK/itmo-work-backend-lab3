package org.ilestegor.applicationservice.exception.exceptions;

public class UserDoesNotBelongsToCompanyException extends RuntimeException{
    public UserDoesNotBelongsToCompanyException() {
    }

    public UserDoesNotBelongsToCompanyException(String message) {
        super(message);
    }
}
