package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyStatsResponse {
    private long totalPolicies;
    private long activePolicies;
    private long expiredPolicies;
    private long cancelledPolicies;
    private long createdPolicies;        // purchased but not yet effective
    private BigDecimal totalCoverageProvided;
    private BigDecimal totalPremiumCollected;
}