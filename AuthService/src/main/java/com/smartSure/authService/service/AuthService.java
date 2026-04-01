package com.smartSure.authService.service;

import com.smartSure.authService.dto.auth.AuthResponseDto;
import com.smartSure.authService.dto.auth.LoginRequestDto;
import com.smartSure.authService.dto.auth.RegisterRequestDto;
import com.smartSure.authService.entity.Role;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.repository.UserRepository;
import com.smartSure.authService.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService handles user registration and login.
 *
 * Security fixes applied:
 * - register() always assigns CUSTOMER role regardless of request payload
 * - createAdmin() is a separate, secured method callable only by ADMIN
 * - login error messages are generic to prevent user enumeration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;

    /**
     * Public self-registration endpoint.
     * Always assigns CUSTOMER role — users cannot self-register as ADMIN.
     */
    @Transactional
    public String register(RegisterRequestDto request) {

        if (repo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = modelMapper.map(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // SECURITY FIX: Always assign CUSTOMER regardless of what was sent
        user.setRole(Role.CUSTOMER);

        repo.save(user);

        log.info("New CUSTOMER registered: {}", request.getEmail());
        return "User registered successfully";
    }

    /**
     * Admin-only endpoint to create any role (CUSTOMER or ADMIN).
     * Must only be called from AdminController which is @PreAuthorize("hasRole('ADMIN')")
     */
    @Transactional
    public String createUser(RegisterRequestDto request) {

        if (repo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = modelMapper.map(request, User.class);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        String roleStr = request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER";
        try {
            user.setRole(Role.valueOf(roleStr));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + roleStr + ". Valid values: CUSTOMER, ADMIN");
        }

        repo.save(user);
        log.info("User created with role {} by admin: {}", user.getRole(), request.getEmail());
        return "User created successfully with role: " + user.getRole().name();
    }

    /**
     * Login — returns JWT token on success.
     * Generic error message to prevent user enumeration.
     */
    public AuthResponseDto login(LoginRequestDto request) {

        User user = repo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getRole().name());
        log.info("Login successful for userId={}, role={}", user.getUserId(), user.getRole());
        return new AuthResponseDto(token, user.getEmail(), user.getRole().name());
    }
}
