package com.smartSure.claimService.dto;

import com.smartSure.claimService.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for Claim operations.
 * ADDED: userId, description, incidentDate fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimResponse {

    private Long id;
    private long policyId;
    private Long userId;
    private BigDecimal amount;
    private Status status;
    private String description;
    private LocalDate incidentDate;
    private LocalDateTime timeOfCreation;

    // Document upload status flags — true means file has been uploaded
    private boolean claimFormUploaded;
    private boolean aadhaarCardUploaded;
    private boolean evidencesUploaded;
}
