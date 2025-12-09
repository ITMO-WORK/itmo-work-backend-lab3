package org.ilestegor.applicationservice.exception.exceptions;

public class InvalidApplicationStatusForApplicationUpdate extends RuntimeException{
    public InvalidApplicationStatusForApplicationUpdate() {
    }

    public InvalidApplicationStatusForApplicationUpdate(String message) {
        super(message);
    }
}
