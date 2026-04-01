package com.smartSure.adminService.dto;

import com.smartSure.adminService.entity.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardMetricsResponse {
    private long totalUsers;
    private long totalPolicies;
    private long totalClaims;
    private long pendingClaims;
    private List<AuditLog> recentActivity;
}
