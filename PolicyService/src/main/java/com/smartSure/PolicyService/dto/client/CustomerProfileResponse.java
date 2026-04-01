package com.smartSure.PolicyService.dto.client;

import lombok.*;

/**
 * Response DTO for customer data fetched from AuthService.
 *
 * FIX: phone changed from String to String (was Long in AuthService causing
 * deserialization failure for numbers with leading zeros or international format).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfileResponse {

    private Long   id;
    private String name;
    private String email;
    private String phone;   // FIX: String (not Long) to handle international numbers
    private Integer age;
}
