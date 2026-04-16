package com.smartSure.paymentService.config;

import com.smartSure.paymentService.security.HeaderAuthenticationFilter;
import com.smartSure.paymentService.security.InternalRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalRequestFilter internalRequestFilter;
    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        //  Public endpoints only
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/actuator/**"
                        ).permitAll()

                        //  EVERYTHING else requires authentication
                        .anyRequest().authenticated()
                )

                //  Internal service validation (X-Internal-Secret)
                .addFilterBefore(internalRequestFilter, UsernamePasswordAuthenticationFilter.class)

                //  Extract user from headers (X-User-Id, X-User-Role)
                .addFilterAfter(headerAuthenticationFilter, InternalRequestFilter.class);

        return http.build();
    }
}