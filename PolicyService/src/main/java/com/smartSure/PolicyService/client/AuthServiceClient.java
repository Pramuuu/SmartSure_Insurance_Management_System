package com.smartSure.PolicyService.client;

import com.smartSure.PolicyService.client.fallback.AuthServiceFallback;
import com.smartSure.PolicyService.dto.client.CustomerProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for AuthService.
 *
 * FIXES APPLIED:
 * 1. Removed hardcoded url="${services.auth-service.url:...}" — this bypassed Eureka discovery.
 *    When both name AND url are specified, the url takes priority and Eureka is ignored.
 *    Now it uses Eureka lb:// load balancing only.
 * 2. Endpoints are under /user/internal/** which are now properly routed through the
 *    gateway route added in ApiGateway application.properties (routes[7]).
 *
 * Fallback strategy: return safe defaults so PolicyService stays operational
 * even when AuthService is temporarily unavailable.
 */
@FeignClient(
        name = "AUTHSERVICE",
        fallback = AuthServiceFallback.class
)
public interface AuthServiceClient {

    @GetMapping("/user/internal/{userId}/profile")
    CustomerProfileResponse getCustomerProfile(@PathVariable("userId") Long userId);

    @GetMapping("/user/internal/{userId}/email")
    String getCustomerEmail(@PathVariable("userId") Long userId);
}