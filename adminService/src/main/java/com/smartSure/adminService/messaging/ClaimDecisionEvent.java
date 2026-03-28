package com.smartSure.adminService.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDecisionEvent {
    private Long claimId;
    private Long policyId;
    private String decision;
    private BigDecimal amount;
    private String customerEmail;
    private String customerName;
    private LocalDateTime decidedAt;
}
