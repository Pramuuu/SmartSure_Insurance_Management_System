package com.smartSure.claimService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.smartSure.claimService.dto.UserResponseDto;

@FeignClient(name = "authservice", path = "/user/internal")
public interface UserClient {

    @GetMapping("/{userId}/profile")
    UserResponseDto getUserById(@PathVariable("userId") Long userId);
}