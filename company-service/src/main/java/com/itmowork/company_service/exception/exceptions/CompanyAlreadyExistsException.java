package com.itmowork.company_service.exception.exceptions;

public class CompanyAlreadyExistsException extends RuntimeException{

    public CompanyAlreadyExistsException(String message){
        super(message);
    }
}
