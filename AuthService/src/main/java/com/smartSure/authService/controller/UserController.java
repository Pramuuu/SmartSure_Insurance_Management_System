package com.smartSure.authService.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartSure.authService.dto.address.AddressRequestDto;
import com.smartSure.authService.dto.address.AddressResponseDto;
import com.smartSure.authService.dto.user.UserRequestDto;
import com.smartSure.authService.dto.user.UserResponseDto;
import com.smartSure.authService.service.AddressService;
import com.smartSure.authService.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Tag(name="Smart Sure User Controller", description="Backend API Testing for User and associated fields")
public class UserController {
	
	private final UserService service;
	private final AddressService addService;
	
	@GetMapping("/profile")
	public String getProfile(HttpServletRequest request) {
	    String userId = request.getHeader("X-User-Id");
	    String role = request.getHeader("X-User-Role");

	    return "UserId: " + userId + ", Role: " + role;
	}
	
	@PostMapping("/addInfo")
	@Operation(summary = "Adding information", description="Adding information to registered user row")
	@ApiResponse(responseCode = "202", description = "Information added successfully")
	public ResponseEntity<UserResponseDto> addInfo(@RequestBody @Valid UserRequestDto reqDto){
		
		UserResponseDto resDto = service.add(reqDto);
		
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(resDto);
	}
	
	@GetMapping("/all")
	@Operation(summary = "Get all users", description = "Admin endpoint to fetch all registered users")
	@ApiResponse(responseCode = "200", description = "Users fetched successfully")
	public ResponseEntity<List<UserResponseDto>> getAllUsers() {
		return ResponseEntity.status(HttpStatus.OK).body(service.getAll());
	}

	@GetMapping("/getInfo/{userId}")
	@Operation(summary = "Get User", description="Get verified user with user id")
	@ApiResponse(responseCode = "200", description = "User fetched successfully")
	public ResponseEntity<UserResponseDto> getUser(@PathVariable Long userId){
		
		UserResponseDto resDto = service.get(userId);
		
		return ResponseEntity.status(HttpStatus.OK).body(resDto);
	}
	
	@PutMapping("/update/{userId}")
	@Operation(summary = "Update User", description="Updating information to registered user row")
	@ApiResponse(responseCode = "202", description = "Information updated successfully")
	public ResponseEntity<UserResponseDto> updateUser(@RequestBody @Valid UserRequestDto reqDto, @PathVariable Long userId){
		
		UserResponseDto resDto = service.update(reqDto, userId);
		
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(resDto);
	}
	
	@DeleteMapping("/delete/{userId}")
	@Operation(summary = "Delete User", description="Removing exixting user from database")
	@ApiResponse(responseCode = "200", description = "User removed successfully")
	public ResponseEntity<UserResponseDto> deleteUser(@PathVariable Long userId){
		
		UserResponseDto resDto = service.delete(userId);
		
		return ResponseEntity.status(HttpStatus.OK).body(resDto);
	}
	
	@PostMapping("/addAddress/{userId}")
	@Operation(summary = "Adding User's Address", description="Adding address to registered user row")
	@ApiResponse(responseCode = "202", description = "Address added successfully")
	public ResponseEntity<AddressResponseDto> addAddress(@RequestBody @Valid AddressRequestDto reqDto, @PathVariable Long userId){
		
		AddressResponseDto resDto = addService.create(reqDto, userId);
		
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(resDto);
	}
	
	@GetMapping("/getAddress/{userId}")
	@Operation(summary = "Get User's Address", description="Get verified user's address with user id")
	@ApiResponse(responseCode = "200", description = "Address fetched successfully")
	public ResponseEntity<AddressResponseDto> getAddress(@PathVariable Long userId){
		
		AddressResponseDto resDto = addService.get(userId);
		
		return ResponseEntity.status(HttpStatus.OK).body(resDto);
	}
	
	@PutMapping("/updateAddress/{userId}")
	@Operation(summary = "Update User's Address", description="Updating address information to registered user row")
	@ApiResponse(responseCode = "202", description = "Address updated successfully")
	public ResponseEntity<AddressResponseDto> updateAddress(@RequestBody @Valid AddressRequestDto reqDto, @PathVariable Long userId){
		
		AddressResponseDto resDto = addService.update(reqDto, userId);
		
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(resDto);
	}
	
	@DeleteMapping("/deleteAddress/{userId}")
	@Operation(summary = "Delete User's address", description="Removing exixting user's address from database")
	@ApiResponse(responseCode = "200", description = "Address removed successfully")
	public ResponseEntity<AddressResponseDto> deleteAddress(@PathVariable Long userId){
		
		AddressResponseDto resDto = addService.delete(userId);
		
		return ResponseEntity.status(HttpStatus.OK).body(resDto);
	}
}
