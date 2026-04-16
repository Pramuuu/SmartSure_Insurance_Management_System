package com.smartSure.claimService.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimDecisionEvent {
    private Long claimId;
    private Long policyId;
    private Long customerId;
    private String decision;       // APPROVED or REJECTED
    private BigDecimal amount;
    private String customerEmail;
    private String customerName;
    private String remarks;
    private LocalDateTime decidedAt;
}
