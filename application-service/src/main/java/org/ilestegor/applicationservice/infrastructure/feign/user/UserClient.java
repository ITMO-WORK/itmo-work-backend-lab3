package org.ilestegor.applicationservice.infrastructure.feign.user;

import org.ilestegor.applicationservice.infrastructure.feign.user.dto.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/user")
public interface UserClient {

    @GetMapping("/{id}")
    UserResponseDto isUserExistsById(@PathVariable UUID id);
}
