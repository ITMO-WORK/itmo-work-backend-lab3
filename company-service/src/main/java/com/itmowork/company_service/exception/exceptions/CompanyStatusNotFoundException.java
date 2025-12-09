package com.itmowork.company_service.exception.exceptions;

public class CompanyStatusNotFoundException extends RuntimeException{

    public CompanyStatusNotFoundException(String message){
        super(message);
    }
}
