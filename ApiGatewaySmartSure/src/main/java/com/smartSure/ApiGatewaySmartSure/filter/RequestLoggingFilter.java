package com.smartSure.ApiGatewaySmartSure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Feature 2: Global Request/Response Logging Filter
 *
 * Logs every request with:
 *  - HTTP method and path
 *  - Authenticated userId and role (from JWT headers injected by JwtAuthFilter)
 *  - Response status code
 *  - Total response time in milliseconds
 *
 * Executes at order 0 — after JwtAuthFilter (order -1) so userId headers are already injected.
 * All logs flow into Loki via the logback-spring.xml appender for Grafana dashboards.
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String method  = request.getMethod().name();
        String path    = request.getURI().getPath();
        String userId  = request.getHeaders().getFirst("X-User-Id");
        String role    = request.getHeaders().getFirst("X-User-Role");
        String reqId   = request.getHeaders().getFirst("X-Request-Id");

        // Log incoming request
        log.info("[REQUEST]  reqId={} method={} path={} userId={} role={}",
                reqId != null ? reqId : "public",
                method, path,
                userId != null ? userId : "anonymous",
                role   != null ? role   : "none");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {

            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatusCode() != null
                    ? response.getStatusCode().value() : 0;

            // Log outgoing response with timing
            if (status >= 400) {
                // Error responses logged at WARN level so they stand out in Grafana
                log.warn("[RESPONSE] reqId={} method={} path={} userId={} status={} duration={}ms",
                        reqId != null ? reqId : "public",
                        method, path,
                        userId != null ? userId : "anonymous",
                        status, duration);
            } else {
                log.info("[RESPONSE] reqId={} method={} path={} userId={} status={} duration={}ms",
                        reqId != null ? reqId : "public",
                        method, path,
                        userId != null ? userId : "anonymous",
                        status, duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        // Must be > -1 (JwtAuthFilter order) so X-User-Id headers are already injected
        // Must be < 1 (default filters) so we wrap the full chain
        return 0;
    }
}
