package com.smartSure.claimService.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user info fetched from AuthService.
 * FIX: Removed 'password' field — it was leaking password hashes in API responses.
 */
@Data
@NoArgsConstructor
public class UserResponseDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    // REMOVED: private String password;  -- security vulnerability
    private String phone;
    private String role;
}
