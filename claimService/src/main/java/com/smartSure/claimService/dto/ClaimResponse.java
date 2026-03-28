package com.smartSure.claimService.dto;

import com.smartSure.claimService.entity.Status;
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
public class ClaimResponse {

    private Long id;
    private long policyId;
    private BigDecimal amount;
    private Status status;
    private LocalDateTime timeOfCreation;

    // Document upload status flags — true means file has been uploaded
    private boolean claimFormUploaded;
    private boolean aadhaarCardUploaded;
    private boolean evidencesUploaded;
}
