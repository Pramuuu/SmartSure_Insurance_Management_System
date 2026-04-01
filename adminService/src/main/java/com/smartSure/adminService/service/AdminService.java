package com.smartSure.adminService.service;

import com.smartSure.adminService.dto.ClaimDTO;
import com.smartSure.adminService.dto.PolicyDTO;
import com.smartSure.adminService.dto.PolicyStatusUpdateRequest;
import com.smartSure.adminService.dto.UserDTO;
import com.smartSure.adminService.entity.AuditLog;
import com.smartSure.adminService.feign.ClaimFeignClient;
import com.smartSure.adminService.feign.PolicyFeignClient;
import com.smartSure.adminService.feign.UserFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ClaimFeignClient claimFeignClient;
    private final PolicyFeignClient policyFeignClient;
    private final UserFeignClient userFeignClient;
    private final AuditLogService auditLogService;

    // ==================== DASHBOARD METRICS ====================
    public com.smartSure.adminService.dto.DashboardMetricsResponse getDashboardMetrics() {
        long totalUsers = userFeignClient.getAllUsers().size();
        long totalPolicies = policyFeignClient.getAllPolicies().size();
        List<ClaimDTO> allClaims = claimFeignClient.getAllClaims();
        long totalClaims = allClaims.size();
        long pendingClaims = allClaims.stream()
                .filter(c -> "UNDER_REVIEW".equals(c.getStatus()) || "SUBMITTED".equals(c.getStatus()))
                .count();

        return com.smartSure.adminService.dto.DashboardMetricsResponse.builder()
                .totalUsers(totalUsers)
                .totalPolicies(totalPolicies)
                .totalClaims(totalClaims)
                .pendingClaims(pendingClaims)
                .recentActivity(auditLogService.getRecentLogs(5))
                .build();
    }

    // ==================== CLAIM MANAGEMENT ====================

    // Get all claims — full admin view
    public List<ClaimDTO> getAllClaims() {
        return claimFeignClient.getAllClaims();
    }

    // Get claims pending admin review
    public List<ClaimDTO> getUnderReviewClaims() {
        return claimFeignClient.getUnderReviewClaims();
    }

    // Get a single claim with its linked policy and user details
    public ClaimDTO getClaimById(Long claimId) {
        return claimFeignClient.getClaimById(claimId);
    }

    // Approve a claim — updates status in claimService and logs the action
    public ClaimDTO approveClaim(Long adminId, Long claimId, String remarks) {
        ClaimDTO updated = claimFeignClient.updateClaimStatus(claimId, "APPROVED");
        auditLogService.log(adminId, "APPROVE_CLAIM", "Claim", claimId, remarks);
        return updated;
    }

    // Reject a claim — updates status in claimService and logs the action
    public ClaimDTO rejectClaim(Long adminId, Long claimId, String remarks) {
        ClaimDTO updated = claimFeignClient.updateClaimStatus(claimId, "REJECTED");
        auditLogService.log(adminId, "REJECT_CLAIM", "Claim", claimId, remarks);
        return updated;
    }

    // Move claim to UNDER_REVIEW — kept for backward compatibility
    // NOTE: With the new submit flow, claims arrive at UNDER_REVIEW automatically
    // after customer calls /submit. This method is now a no-op if claim is already UNDER_REVIEW.
    public ClaimDTO markUnderReview(Long adminId, Long claimId) {
        ClaimDTO claim = claimFeignClient.getClaimById(claimId);
        // Only attempt transition if claim is still in SUBMITTED state
        if ("SUBMITTED".equals(claim.getStatus())) {
            ClaimDTO updated = claimFeignClient.updateClaimStatus(claimId, "UNDER_REVIEW");
            auditLogService.log(adminId, "MARK_UNDER_REVIEW", "Claim", claimId, "Claim moved to under review");
            return updated;
        }
        // Already UNDER_REVIEW — just log and return current state
        auditLogService.log(adminId, "MARK_UNDER_REVIEW", "Claim", claimId, "Claim already under review");
        return claim;
    }

    // ==================== POLICY MANAGEMENT ====================

    // Get all policies
    public List<PolicyDTO> getAllPolicies() {
        return policyFeignClient.getAllPolicies();
    }

    // Get a single policy by ID — passes adminId and ADMIN role so policyService skips ownership check
    public PolicyDTO getPolicyById(Long policyId) {
        return policyFeignClient.getPolicyById(policyId);
    }

    // Cancel a policy — uses admin status update endpoint with CANCELLED status
    public PolicyDTO cancelPolicy(Long adminId, Long policyId, String reason) {
        PolicyStatusUpdateRequest req = new PolicyStatusUpdateRequest("CANCELLED", reason);
        PolicyDTO updated = policyFeignClient.updatePolicyStatus(policyId, req);
        auditLogService.log(adminId, "CANCEL_POLICY", "Policy", policyId, reason);
        return updated;
    }

    // ==================== USER MANAGEMENT ====================

    // Get all users
    public List<UserDTO> getAllUsers() {
        return userFeignClient.getAllUsers();
    }

    // Get a single user by ID
    public UserDTO getUserById(Long userId) {
        return userFeignClient.getUserById(userId);
    }

    // ==================== AUDIT LOGS ====================

    // Get recent activity feed — for admin dashboard
    public List<AuditLog> getRecentActivity(int limit) {
        return auditLogService.getRecentLogs(limit);
    }

    // Get full audit trail for a specific claim or policy
    public List<AuditLog> getEntityHistory(String entity, Long id) {
        return auditLogService.getLogsByEntityAndId(entity, id);
    }
}
