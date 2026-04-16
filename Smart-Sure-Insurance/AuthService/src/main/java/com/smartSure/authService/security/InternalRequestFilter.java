package com.smartSure.authService.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class InternalRequestFilter extends OncePerRequestFilter {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Public endpoints — Gateway does NOT inject X-Internal-Secret for these paths
        if (uri.equals("/api/auth/login") ||
                uri.equals("/api/auth/register") ||
                uri.equals("/api/auth/verify-otp") ||
                uri.startsWith("/actuator") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String secret = request.getHeader("X-Internal-Secret");

        if (!internalSecret.equals(secret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden: invalid internal secret\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}