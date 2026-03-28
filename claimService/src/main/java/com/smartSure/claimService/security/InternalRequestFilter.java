package com.smartSure.claimService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalRequestFilter extends OncePerRequestFilter {
	
	@Value("${internal.secret}")
	private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
    	
    	if (request.getRequestURI().startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow Prometheus scraping — no internal secret required for actuator
    	if (request.getRequestURI().startsWith("/actuator")) {
    	    filterChain.doFilter(request, response);
    	    return;
    	}

        String secret = request.getHeader("X-Internal-Secret");

        if (!internalSecret.equals(secret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}