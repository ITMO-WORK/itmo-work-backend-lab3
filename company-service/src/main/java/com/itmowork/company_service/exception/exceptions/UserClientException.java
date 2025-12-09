package com.itmowork.company_service.exception.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserClientException extends RuntimeException{

    private final HttpStatus status;
    public UserClientException(String message, HttpStatus status){
        super(message);
        this.status = status;
    }

}
