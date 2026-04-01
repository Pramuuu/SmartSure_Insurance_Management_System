package com.smartSure.claimService.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a claim.
 * ADDED: description and incidentDate for proper claim documentation.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClaimRequest {

    @NotNull(message = "Policy ID is required")
    private Long policyId;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String incidentDate;  // ISO date string e.g. "2024-03-25"
}
