package com.smartSure.authService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smartSure.authService.security.HeaderAuthenticationFilter;
import com.smartSure.authService.security.InternalRequestFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final InternalRequestFilter internalRequestFilter;
	private final HeaderAuthenticationFilter headerAuthenticationFilter;
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
		http.csrf(csrf -> csrf.disable())
		.cors(cors -> cors.configurationSource(corsConfigurationSource()))
		.httpBasic(httpBasic -> httpBasic.disable()) 
        .formLogin(form -> form.disable()) 
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth -> auth 
				.requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/verify-otp"
                ).permitAll()
				.requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
				.requestMatchers("/actuator/**").permitAll()   //just for testing, will change later
		        .anyRequest().permitAll())
		.addFilterBefore(internalRequestFilter, UsernamePasswordAuthenticationFilter.class)
		.addFilterAfter(headerAuthenticationFilter, InternalRequestFilter.class);
		
		return http.build();
	}

	@Bean
	public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
		org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
		config.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://localhost:4200"));
		config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(java.util.List.of("*"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
