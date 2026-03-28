package com.smartSure.authService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartSure.authService.dto.auth.AuthResponseDto;
import com.smartSure.authService.dto.auth.LoginRequestDto;
import com.smartSure.authService.dto.auth.RegisterRequestDto;
import com.smartSure.authService.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

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
	
	@PostMapping("/login")
	@Operation(summary = "Login User", description="Verifying user credentials and generating JWT token")
	@ApiResponse(responseCode = "200", description = "User verified successfully")
	public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request){
		return ResponseEntity.ok(authService.login(request));
	}
}