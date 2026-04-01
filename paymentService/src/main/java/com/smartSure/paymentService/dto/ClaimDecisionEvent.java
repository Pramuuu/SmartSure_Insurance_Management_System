package com.smartSure.paymentService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDecisionEvent {
    private Long claimId;
    private Long policyId;
    private String decision; // APPROVED or REJECTED
    private BigDecimal amount;
    private String customerEmail;
    private String customerName;
    private LocalDateTime decidedAt;
}
