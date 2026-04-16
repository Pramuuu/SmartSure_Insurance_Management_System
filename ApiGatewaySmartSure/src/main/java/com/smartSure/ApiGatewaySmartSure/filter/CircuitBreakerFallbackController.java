package com.smartSure.ApiGatewaySmartSure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Feature 4: Circuit Breaker Fallback Controller
 *
 * When Resilience4j circuit breaker opens (service is down or too slow),
 * the Gateway redirects to these fallback endpoints instead of hanging.
 *
 * Each service has its own fallback endpoint that returns a specific,
 * meaningful error message — not a generic 500 error.
 *
 * Fallback endpoints are internal — they are NOT routed through the Gateway
 * and are NOT accessible from outside. They only respond to internal
 * circuit breaker redirects.
 */
@Slf4j
@RestController
public class CircuitBreakerFallbackController {

    /**
     * Fallback for PolicyService — /api/policies/** and /api/policy-types/**
     */
    @RequestMapping("/fallback/policy")
    public Mono<ResponseEntity<Map<String, Object>>> policyFallback(ServerWebExchange exchange) {
        log.error("[CIRCUIT-BREAKER] PolicyService is unavailable — serving fallback");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Policy service is temporarily unavailable",
                "message", "Our policy service is currently under maintenance. Please try again in a few minutes.",
                "service", "policyservice",
                "timestamp", LocalDateTime.now().toString(),
                "status", 503
        )));
    }

    /**
     * Fallback for ClaimService — /api/claims/**
     */
    @RequestMapping("/fallback/claim")
    public Mono<ResponseEntity<Map<String, Object>>> claimFallback(ServerWebExchange exchange) {
        log.error("[CIRCUIT-BREAKER] ClaimService is unavailable — serving fallback");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Claim service is temporarily unavailable",
                "message", "Our claims service is currently under maintenance. Your claim data is safe. Please try again shortly.",
                "service", "claimservice",
                "timestamp", LocalDateTime.now().toString(),
                "status", 503
        )));
    }

    /**
     * Fallback for PaymentService — /api/payments/**
     */
    @RequestMapping("/fallback/payment")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback(ServerWebExchange exchange) {
        log.error("[CIRCUIT-BREAKER] PaymentService is unavailable — serving fallback");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Payment service is temporarily unavailable",
                "message", "Our payment service is currently under maintenance. No payment has been processed. Please try again later.",
                "service", "paymentservice",
                "timestamp", LocalDateTime.now().toString(),
                "status", 503
        )));
    }

    /**
     * Fallback for AdminService — /api/admin/**
     */
    @RequestMapping("/fallback/admin")
    public Mono<ResponseEntity<Map<String, Object>>> adminFallback(ServerWebExchange exchange) {
        log.error("[CIRCUIT-BREAKER] AdminService is unavailable — serving fallback");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Admin service is temporarily unavailable",
                "message", "The admin dashboard service is currently under maintenance. Please try again shortly.",
                "service", "adminservice",
                "timestamp", LocalDateTime.now().toString(),
                "status", 503
        )));
    }

    /**
     * Fallback for AuthService — /api/auth/**
     */
    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(ServerWebExchange exchange) {
        log.error("[CIRCUIT-BREAKER] AuthService is unavailable — serving fallback");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Authentication service is temporarily unavailable",
                "message", "Login and registration are temporarily unavailable. Please try again in a few minutes.",
                "service", "authservice",
                "timestamp", LocalDateTime.now().toString(),
                "status", 503
        )));
    }
}
