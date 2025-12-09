package com.itmowork.company_service.dto.request;


public record UserRequestDto(
        String fullName,
        String password,
        String email
) {
}
