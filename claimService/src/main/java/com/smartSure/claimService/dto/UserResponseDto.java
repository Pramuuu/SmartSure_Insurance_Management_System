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
    private Long id;          // InternalAuthController returns 'id' not 'userId'
    private String name;      // returns full name as single 'name' field
    private String email;
    private String phone;
}
