package com.smartSure.authService.controller;

import com.smartSure.authService.dto.address.AddressRequestDto;
import com.smartSure.authService.dto.address.AddressResponseDto;
import com.smartSure.authService.dto.auth.RegisterRequestDto;
import com.smartSure.authService.dto.user.UserRequestDto;
import com.smartSure.authService.dto.user.UserResponseDto;
import com.smartSure.authService.service.AddressService;
import com.smartSure.authService.service.AuthService;
import com.smartSure.authService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Tag(name = "Smart Sure User Controller", description = "User management endpoints")
public class UserController {

    private final UserService service;
    private final AddressService addService;
    private final AuthService authService;

    // ================================================================
    // PROFILE
    // ================================================================

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile summary")
    public ResponseEntity<String> getProfile(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");
        return ResponseEntity.ok("UserId: " + userId + ", Role: " + role);
    }

    // ================================================================
    // ADMIN-ONLY endpoints
    // ================================================================

    /**
     * Admin creates a user with any role (CUSTOMER or ADMIN).
     * This is the ONLY way to create an ADMIN account.
     */
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin creates a user with specified role (ADMIN only)")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    public ResponseEntity<String> adminCreateUser(@RequestBody @Valid RegisterRequestDto reqDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(reqDto));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all users (Admin only)")
    @ApiResponse(responseCode = "200", description = "Users fetched successfully")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(service.getAll());
    }

    @DeleteMapping("/delete/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete user (Admin only)")
    @ApiResponse(responseCode = "200", description = "User removed successfully")
    public ResponseEntity<UserResponseDto> deleteUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.delete(userId));
    }

    // ================================================================
    // SELF-MANAGEMENT (Customer or Admin about their own data)
    // ================================================================

    @PostMapping("/addInfo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add user profile info")
    @ApiResponse(responseCode = "202", description = "Information added successfully")
    public ResponseEntity<UserResponseDto> addInfo(@RequestBody @Valid UserRequestDto reqDto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.add(reqDto));
    }

    @GetMapping("/getInfo/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user by ID")
    @ApiResponse(responseCode = "200", description = "User fetched successfully")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.get(userId));
    }

    @PutMapping("/update/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user info")
    @ApiResponse(responseCode = "202", description = "Information updated successfully")
    public ResponseEntity<UserResponseDto> updateUser(@RequestBody @Valid UserRequestDto reqDto,
                                                      @PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.update(reqDto, userId));
    }

    // ================================================================
    // ADDRESS MANAGEMENT
    // ================================================================

    @PostMapping("/addAddress/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add user address")
    @ApiResponse(responseCode = "202", description = "Address added successfully")
    public ResponseEntity<AddressResponseDto> addAddress(@RequestBody @Valid AddressRequestDto reqDto,
                                                         @PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(addService.create(reqDto, userId));
    }

    @GetMapping("/getAddress/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user address")
    @ApiResponse(responseCode = "200", description = "Address fetched successfully")
    public ResponseEntity<AddressResponseDto> getAddress(@PathVariable Long userId) {
        return ResponseEntity.ok(addService.get(userId));
    }

    @PutMapping("/updateAddress/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user address")
    @ApiResponse(responseCode = "202", description = "Address updated successfully")
    public ResponseEntity<AddressResponseDto> updateAddress(@RequestBody @Valid AddressRequestDto reqDto,
                                                            @PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(addService.update(reqDto, userId));
    }

    @DeleteMapping("/deleteAddress/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete user address")
    @ApiResponse(responseCode = "200", description = "Address removed successfully")
    public ResponseEntity<AddressResponseDto> deleteAddress(@PathVariable Long userId) {
        return ResponseEntity.ok(addService.delete(userId));
    }
}
