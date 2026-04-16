package com.smartSure.ApiGatewaySmartSure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT Authentication Filter — executes first (order = -1)
 *
 * Responsibilities:
 *  1. Skip public paths (no JWT required)
 *  2. Extract and validate JWT from Authorization header
 *  3. Inject X-User-Id, X-User-Role, X-Internal-Secret, X-Request-Id headers
 *     for downstream services and for RoleAuthorizationFilter (order = 1)
 *  4. Reject with 401 if token missing or invalid
 *
 * Public paths — NO JWT required:
 *  /api/auth/**                     — login, register
 *  /api/auth/verify-otp             — 2FA OTP verification (step 2 of login)
 *  /api/policy-types/**             — public catalog
 *  /api/policies/calculate-premium  — public premium calculator
 *  /fallback/**                     — circuit breaker fallback endpoints (internal)
 *  /swagger-ui, /v3/api-docs        — API documentation
 *  /actuator/**                     — health checks
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${internal.secret}")
    private String internalSecret;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/verify-otp",
            "/api/policies/calculate-premium",
            "/api/policies/seed",
            "/fallback",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip JWT validation for public paths
        if (isPublicPath(request)) {
            log.debug("[JWT-FILTER] Public path — skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        log.debug("[JWT-FILTER] Secured path — validating JWT: {}", path);
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT-FILTER] Missing or malformed Authorization header for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Authorization token required");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseToken(token);

            String userId = claims.getSubject();
            String role   = String.valueOf(claims.get("role"));

            if (userId == null || "null".equals(userId)) {
                log.error("[JWT-FILTER] JWT Subject (userId) is null for path: {}", path);
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid user identification in token");
            }

            log.debug("[JWT-FILTER] Authenticated — userId={}, role={}, path={}", userId, role, path);

            // Inject security headers for downstream services and RoleAuthorizationFilter
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",        userId)
                    .header("X-User-Role",       role)
                    .header("X-Internal-Secret", internalSecret)
                    .header("X-Request-Id",      generateRequestId())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("[JWT-FILTER] Expired JWT for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Token has expired. Please login again.");
        } catch (MalformedJwtException | UnsupportedJwtException | SecurityException e) {
            log.warn("[JWT-FILTER] Invalid JWT for path: {} — {}", path, e.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token format");
        } catch (Exception e) {
            log.error("[JWT-FILTER] JWT processing error for path: {} — {}", path, e.getMessage());
            return reject(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication processing error");
        }
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
        		.verifyWith(key)
        		.build()
        		.parseSignedClaims(token)
        		.getPayload();
    }

    private boolean isPublicPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        // Always allow CORS preflight requests
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // /api/policy-types/all requires admin, it's not public
        if (path.startsWith("/api/policy-types/all")) {
            return false;
        }

        // Only GET requests to /api/policy-types are public
        if (path.startsWith("/api/policy-types") && "GET".equals(method)) {
            return true;
        }

        return PUBLIC_PATHS.stream().anyMatch(prefix ->
                path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public int getOrder() {
        return -1; // First filter to execute — before logging (0) and role auth (1)
    }
}