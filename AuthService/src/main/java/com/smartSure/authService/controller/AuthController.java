package com.smartSure.authService.controller;

import com.smartSure.authService.dto.auth.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartSure.authService.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name="Smart Sure Auth Controller", description="Backend API Testing for User Registration and Login")
public class AuthController {
	
	private final AuthService authService;
	
	@PostMapping("/register")
	@Operation(summary = "Register User", description="Adding a new user in the database")
	@ApiResponse(responseCode = "200", description = "User added successfully")
	public ResponseEntity<String> register(@RequestBody RegisterRequestDto request){
		return ResponseEntity.ok(authService.register(request));
	}
	
//	@PostMapping("/login")
//	@Operation(summary = "Login User", description="Verifying user credentials and generating JWT token")
//	@ApiResponse(responseCode = "200", description = "User verified successfully")
//	public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request){
//		return ResponseEntity.ok(authService.login(request));
//	}


	// Change login endpoint return type
	@PostMapping("/login")
	@Operation(summary = "Step 1: Verify credentials and send OTP")
	public ResponseEntity<PreAuthResponseDto> login(@RequestBody @Valid LoginRequestDto request) {
		return ResponseEntity.ok(authService.login(request));
	}

	// Add new endpoint
	@PostMapping("/verify-otp")
	@Operation(summary = "Step 2: Verify OTP and receive JWT token")
	public ResponseEntity<AuthResponseDto> verifyOtp(@RequestBody @Valid VerifyOtpRequestDto request) {
		return ResponseEntity.ok(authService.verifyOtp(request));
	}
	
	@PostMapping("/admin/create")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Create Admin User", description="Admin creates a new admin user")
	@ApiResponse(responseCode = "201", description = "Admin created successfully")
	public ResponseEntity<String> createAdmin(@RequestBody @Valid AdminCreateRequestDto request){
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.createAdmin(request));
	}
}