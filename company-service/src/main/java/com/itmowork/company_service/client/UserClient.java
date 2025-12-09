package com.itmowork.company_service.client;

import com.itmowork.company_service.configuration.FeignConfig;
import com.itmowork.company_service.dto.request.UserRequestDto;
import com.itmowork.company_service.dto.response.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {

    @PostMapping("/api/user/create")
    UserResponseDto createUser(@RequestBody UserRequestDto userRequestDto);

    @GetMapping("/api/user/{id}")
    UserResponseDto findUserById(@PathVariable UUID id);



}
