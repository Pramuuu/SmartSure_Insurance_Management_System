package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {
    private long totalUsers;
    private long totalPolicies;
    private long totalClaims;
    private long pendingClaims;          // SUBMITTED + UNDER_REVIEW
    private List<AuditLogDTO> recentActivity;  // FIXED: was List<AuditLog> entity
}