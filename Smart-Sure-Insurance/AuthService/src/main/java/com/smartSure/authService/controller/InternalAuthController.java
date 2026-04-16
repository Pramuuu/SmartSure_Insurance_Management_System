package com.smartSure.authService.controller;

import com.smartSure.authService.dto.client.CustomerProfileResponse;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-only endpoints for service-to-service communication.
 * Protected by InternalRequestFilter (X-Internal-Secret header).
 * NOT exposed to end users via the Gateway (except via /user/internal/** route
 * which requires valid JWT from gateway).
 *
 * FIX: phone converted to String for safe Feign deserialization.
 */
@Slf4j
@RestController
@RequestMapping("/user/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserRepository userRepository;

    /**
     * Get user email by userId — used by PolicyService for notifications
     */
    @GetMapping("/{userId}/email")
    public ResponseEntity<String> getUserEmail(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        log.debug("Internal email lookup for userId={}", userId);
        return ResponseEntity.ok(user.getEmail());
    }

    /**
     * Get full user profile by userId — used by PolicyService for customer info
     * FIX: phone returned as String to match PolicyService CustomerProfileResponse.phone type
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<CustomerProfileResponse> getProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String phone = user.getPhone() != null ? String.valueOf(user.getPhone()) : null;
        String name  = (user.getFirstName() != null ? user.getFirstName() : "") +
                       (user.getLastName()  != null ? " " + user.getLastName() : "");

        CustomerProfileResponse response = CustomerProfileResponse.builder()
                .id(user.getUserId())
                .name(name.trim())
                .email(user.getEmail())
                .phone(phone)        // FIX: String not Long
                .build();

        log.debug("Internal profile lookup for userId={}", userId);
        return ResponseEntity.ok(response);
    }
}