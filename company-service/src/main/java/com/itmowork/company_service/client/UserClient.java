package com.itmowork.company_service.client;

import com.itmowork.company_service.configuration.FeignConfig;
import com.itmowork.company_service.dto.request.UserRequestDto;
import com.itmowork.company_service.dto.response.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;

import java.util.UUID;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {

    @PostMapping("/api/auth/register-company-owner")
    UserResponseDto registerCompanyOwner(@RequestBody UserRequestDto userRequestDto, @RequestHeader(HttpHeaders.AUTHORIZATION) String token);

    @GetMapping("/api/user/{id}")
    UserResponseDto findUserById(@PathVariable UUID id);



}
