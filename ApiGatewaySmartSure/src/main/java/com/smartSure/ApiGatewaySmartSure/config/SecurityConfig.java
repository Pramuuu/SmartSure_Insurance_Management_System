package com.smartSure.ApiGatewaySmartSure.config;

import com.smartSure.ApiGatewaySmartSure.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // Option 1 — use String and split manually:
    @Value("${cors.allowed.origins}")
    private String allowedOriginsRaw;



    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        String[] origins = allowedOriginsRaw.split(",");

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**",
                                // Allow Prometheus scraping — no JWT required for actuator endpoints
                                "/actuator/**").permitAll()
                        .anyExchange().permitAll()
                )
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
