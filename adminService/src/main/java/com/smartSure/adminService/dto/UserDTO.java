package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    // From /user/internal/{userId}/profile (CustomerProfileResponse)
    private Long id;           // ← FIXED: was userId, internal endpoint returns id
    private String name;       // ← FIXED: was firstName+lastName, internal returns full name

    // From /user/all (UserResponseDto)
    private Long userId;       // present when from /user/all
    private String firstName;  // present when from /user/all
    private String lastName;   // present when from /user/all
    private String role;       // present when from /user/all

    // Common to both endpoints
    private String email;
    private String phone;      // ← FIXED: was Long, InternalAuthController returns String

    /**
     * Returns the best available display name regardless of which endpoint populated this DTO.
     * Call this instead of getName() or getFirstName() directly.
     */
    public String getDisplayName() {
        if (name != null && !name.isBlank()) return name;
        if (firstName != null) {
            return lastName != null ? firstName + " " + lastName : firstName;
        }
        return "Unknown";
    }

    /**
     * Returns the best available ID regardless of which endpoint populated this DTO.
     */
    public Long getEffectiveId() {
        return id != null ? id : userId;
    }
}