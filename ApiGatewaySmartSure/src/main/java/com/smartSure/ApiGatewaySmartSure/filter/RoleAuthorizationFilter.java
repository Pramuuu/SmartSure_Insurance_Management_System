package com.smartSure.ApiGatewaySmartSure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Feature 3: Role-Based Authorization Filter at Gateway Level
 *
 * Blocks requests to role-restricted routes BEFORE they reach downstream services.
 * This prevents:
 *  - CUSTOMER users from calling /api/admin/** endpoints
 *  - Unauthenticated users from reaching any protected route
 *
 * Executes at order 1 — after JwtAuthFilter (-1) and LoggingFilter (0),
 * so X-User-Role is already available in headers.
 *
 * Route access rules:
 *  /api/admin/**    → ADMIN only
 *  /api/claims/**   → CUSTOMER or ADMIN
 *  /api/policies/** → CUSTOMER or ADMIN
 *  /api/payments/** → CUSTOMER or ADMIN
 *  /user/**         → CUSTOMER or ADMIN (own profile management)
 *  /api/auth/**     → public (handled by JwtAuthFilter skip)
 *  /api/policy-types/** → public (handled by JwtAuthFilter skip)
 */
@Slf4j
@Component
public class RoleAuthorizationFilter implements GlobalFilter, Ordered {

    // Routes that require ADMIN role exclusively
    private static final List<String> ADMIN_ONLY_PATHS = List.of(
            "/api/admin"
    );

    // Routes that require at least one of these roles
    // (both CUSTOMER and ADMIN can access)
    private static final List<String> AUTHENTICATED_PATHS = List.of(
            "/api/claims",
            "/api/policies",
            "/api/payments",
            "/user"
    );

    // Fully public paths — skip all role checks
    // These must match the PUBLIC_PATHS in JwtAuthFilter
    // AFTER — paste this in its place
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/verify-otp",
            "/api/policy-types",
            "/api/policies/calculate-premium",
            "/api/policies/seed",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/fallback"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath().replaceAll("/+$", "");
        String role = request.getHeaders().getFirst("X-User-Role");
        String method = request.getMethod().name();

        // Always allow CORS preflight requests
        if ("OPTIONS".equals(method)) {
            return chain.filter(exchange);
        }

        // Skip role check for public paths — JwtAuthFilter already handles these
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // At this point, JwtAuthFilter has already validated the JWT.
        // X-User-Role header is present if user is authenticated.
        if (role == null || role.isBlank()) {
            log.warn("[ROLE-AUTH] No role header found for path={} — rejecting", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        // Check ADMIN-only routes
        if (isAdminOnlyPath(path)) {
            if (!"ADMIN".equals(role)) {
                log.warn("[ROLE-AUTH] CUSTOMER attempted ADMIN path={} — blocked at Gateway", path);
                return reject(exchange, HttpStatus.FORBIDDEN,
                        "Access denied. Administrator privileges required.");
            }
            log.debug("[ROLE-AUTH] ADMIN access granted path={}", path);
            return chain.filter(exchange);
        }

        // Check authenticated routes (CUSTOMER or ADMIN)
        if (isAuthenticatedPath(path)) {
            if (!"CUSTOMER".equals(role) && !"ADMIN".equals(role)) {
                log.warn("[ROLE-AUTH] Unknown role={} attempted path={} — blocked", role, path);
                return reject(exchange, HttpStatus.FORBIDDEN,
                        "Access denied. Invalid role.");
            }
            log.debug("[ROLE-AUTH] {} access granted path={}", role, path);
            return chain.filter(exchange);
        }

        // Unknown path — allow through (downstream service handles it)
        return chain.filter(exchange);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(prefix ->
                path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private boolean isAdminOnlyPath(String path) {
        return ADMIN_ONLY_PATHS.stream().anyMatch(prefix ->
                path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private boolean isAuthenticatedPath(String path) {
        return AUTHENTICATED_PATHS.stream().anyMatch(prefix ->
                path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        // After JwtAuthFilter (-1) and LoggingFilter (0)
        return 1;
    }
}