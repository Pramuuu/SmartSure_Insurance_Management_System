package com.smartSure.ApiGatewaySmartSure.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Primary key resolver — identifies each user by userId from JWT headers.
     * Falls back to remote IP for public/unauthenticated routes.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * PRIMARY rate limiter — used as default by GatewayAutoConfiguration.
     * Customer routes: 10 requests/second, burst up to 20.
     */
    @Bean
    @Primary                          // ← THIS fixes the startup error
    public RedisRateLimiter customerRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Admin rate limiter — referenced explicitly in properties as #{@adminRateLimiter}
     * 50 requests/second, burst up to 100.
     */
    @Bean
    public RedisRateLimiter adminRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }

    /**
     * Auth rate limiter — referenced explicitly as #{@authRateLimiter}
     * Deliberately low to prevent brute force: 3/second, burst 5.
     */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(3, 5, 1);
    }
}