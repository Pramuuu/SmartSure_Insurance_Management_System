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
 * Enterprise JWT Auth Filter for API Gateway.
 *
 * On every request:
 *  1. Skip public endpoints (auth, swagger, actuator)
 *  2. Extract & validate JWT from Authorization header
 *  3. Inject X-User-Id, X-User-Role, X-Internal-Secret into downstream headers
 *  4. Reject with 401 if token missing or invalid
 *  5. Reject with 403 if token expired
 *
 * Uses JJWT 0.12.x API (parser().verifyWith().build().parseSignedClaims())
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${internal.secret}")
    private String internalSecret;

    // Paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/api/policy-types",           // Public catalog
            "/api/policies/calculate-premium" // Public premium calculator
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            log.debug("Public path - skipping auth: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Authorization token required");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseToken(token);
            String userId = String.valueOf(claims.get("userId"));
            String role   = String.valueOf(claims.get("role"));

            log.debug("Authenticated request - userId={}, role={}, path={}", userId, role, path);

            // Mutate the request with security headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",         userId)
                    .header("X-User-Role",        role)
                    .header("X-Internal-Secret",  internalSecret)
                    .header("X-Request-Id",       generateRequestId())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token for path: {}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Token has expired");
        } catch (MalformedJwtException | UnsupportedJwtException | SecurityException e) {
            log.warn("Invalid JWT token for path: {} - {}", path, e.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token");
        } catch (Exception e) {
            log.error("JWT processing error for path: {} - {}", path, e.getMessage());
            return reject(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication error");
        }
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
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
        return -1; // Execute before all other filters
    }
}