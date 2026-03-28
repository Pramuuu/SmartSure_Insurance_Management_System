package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDTO {
    private Long id;
    private Long policyId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timeOfCreation;
    private boolean claimFormUploaded;
    private boolean aadhaarCardUploaded;
    private boolean evidencesUploaded;
}
