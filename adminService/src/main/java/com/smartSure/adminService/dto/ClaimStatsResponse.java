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
public class ClaimStatsResponse {
    private long totalClaims;
    private long draftClaims;
    private long submittedClaims;
    private long underReviewClaims;
    private long approvedClaims;
    private long rejectedClaims;
    private long closedClaims;
    private long autoApprovedClaims;
    private BigDecimal totalApprovedAmount;   // sum of all approved claim amounts
    private BigDecimal totalPendingAmount;    // sum of submitted + under_review amounts
}