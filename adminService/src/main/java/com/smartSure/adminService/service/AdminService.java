package com.smartSure.adminService.service;

import com.smartSure.adminService.dto.*;
import com.smartSure.adminService.feign.ClaimFeignClient;
import com.smartSure.adminService.feign.PaymentFeignClient;
import com.smartSure.adminService.feign.PolicyFeignClient;
import com.smartSure.adminService.feign.UserFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ClaimFeignClient claimFeignClient;
    private final PolicyFeignClient policyFeignClient;
    private final UserFeignClient userFeignClient;
    private final PaymentFeignClient paymentFeignClient;
    private final AuditLogService auditLogService;

    // ══════════════════════════════════════════════════════════
    // DASHBOARD
    // ══════════════════════════════════════════════════════════

    @Cacheable(value = "dashboard-data", key = "'metrics'")
    public DashboardMetricsResponse getDashboardMetrics() {
        log.info("Loading dashboard metrics from downstream services");

        // Each Feign call is wrapped individually — a single down service returns 0
        // instead of crashing the whole dashboard with a 500 error.
        long totalUsers = 0;
        try { totalUsers = userFeignClient.getAllUsers().size(); }
        catch (Exception e) { log.warn("[Dashboard] UserService unavailable: {}", e.getMessage()); }

        long totalPolicies = 0;
        try { totalPolicies = policyFeignClient.getAllPolicies().size(); }
        catch (Exception e) { log.warn("[Dashboard] PolicyService unavailable: {}", e.getMessage()); }

        List<ClaimDTO> allClaims = List.of();
        try { allClaims = claimFeignClient.getAllClaims(); }
        catch (Exception e) { log.warn("[Dashboard] ClaimService unavailable: {}", e.getMessage()); }

        long totalClaims   = allClaims.size();
        long pendingClaims = allClaims.stream()
                .filter(c -> "UNDER_REVIEW".equals(c.getStatus())
                        || "SUBMITTED".equals(c.getStatus()))
                .count();

        return DashboardMetricsResponse.builder()
                .totalUsers(totalUsers)
                .totalPolicies(totalPolicies)
                .totalClaims(totalClaims)
                .pendingClaims(pendingClaims)
                .recentActivity(auditLogService.getRecentLogs(5))
                .build();
    }

    @Cacheable(value = "dashboard-data", key = "'policy-stats'")
    public PolicyStatsResponse getPolicyStats() {
        log.info("Loading policy stats");
        List<PolicyDTO> all = List.of();
        try { all = policyFeignClient.getAllPolicies(); }
        catch (Exception e) { log.warn("[Dashboard] PolicyService unavailable for stats: {}", e.getMessage()); }

        long active    = all.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();
        long expired   = all.stream().filter(p -> "EXPIRED".equals(p.getStatus())).count();
        long cancelled = all.stream().filter(p -> "CANCELLED".equals(p.getStatus())).count();
        long created   = all.stream().filter(p -> "CREATED".equals(p.getStatus())).count();

        BigDecimal totalCoverage = all.stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .map(PolicyDTO::getCoverageAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPremium = all.stream()
                .map(PolicyDTO::getPremiumAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PolicyStatsResponse.builder()
                .totalPolicies(all.size())
                .activePolicies(active)
                .expiredPolicies(expired)
                .cancelledPolicies(cancelled)
                .createdPolicies(created)
                .totalCoverageProvided(totalCoverage)
                .totalPremiumCollected(totalPremium)
                .build();
    }

    @Cacheable(value = "dashboard-data", key = "'claim-stats'")
    public ClaimStatsResponse getClaimStats() {
        log.info("Loading claim stats");
        List<ClaimDTO> all = List.of();
        try { all = claimFeignClient.getAllClaims(); }
        catch (Exception e) { log.warn("[Dashboard] ClaimService unavailable for stats: {}", e.getMessage()); }

        long draft       = count(all, "DRAFT");
        long submitted   = count(all, "SUBMITTED");
        long underReview = count(all, "UNDER_REVIEW");
        long approved    = count(all, "APPROVED");
        long rejected    = count(all, "REJECTED");
        long closed      = count(all, "CLOSED");
        long autoApproved = all.stream().filter(ClaimDTO::isAutoApproved).count();

        BigDecimal approvedAmount = all.stream()
                .filter(c -> "APPROVED".equals(c.getStatus()))
                .map(ClaimDTO::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingAmount = all.stream()
                .filter(c -> "SUBMITTED".equals(c.getStatus())
                        || "UNDER_REVIEW".equals(c.getStatus()))
                .map(ClaimDTO::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ClaimStatsResponse.builder()
                .totalClaims(all.size())
                .draftClaims(draft)
                .submittedClaims(submitted)
                .underReviewClaims(underReview)
                .approvedClaims(approved)
                .rejectedClaims(rejected)
                .closedClaims(closed)
                .autoApprovedClaims(autoApproved)
                .totalApprovedAmount(approvedAmount)
                .totalPendingAmount(pendingAmount)
                .build();
    }

    @CacheEvict(value = "dashboard-data", allEntries = true)
    public void evictDashboardCache() {
        log.debug("Dashboard cache evicted");
    }

    // ══════════════════════════════════════════════════════════
    // CLAIM MANAGEMENT
    // ══════════════════════════════════════════════════════════

    public List<ClaimDTO> getAllClaims() {
        return claimFeignClient.getAllClaims();
    }

    // Admin work queue — SUBMITTED + UNDER_REVIEW together
    public List<ClaimDTO> getPendingClaims() {
        return claimFeignClient.getAllClaims().stream()
                .filter(c -> "SUBMITTED".equals(c.getStatus())
                        || "UNDER_REVIEW".equals(c.getStatus()))
                .collect(Collectors.toList());
    }

    public List<ClaimDTO> getUnderReviewClaims() {
        return claimFeignClient.getUnderReviewClaims();
    }

    public ClaimDTO getClaimById(Long claimId) {
        return claimFeignClient.getClaimById(claimId);
    }

    public ClaimDTO approveClaim(Long adminId, Long claimId, String remarks) {
        ClaimStatusUpdateRequest req = new ClaimStatusUpdateRequest("APPROVED", remarks);
        ClaimDTO updated = claimFeignClient.updateClaimStatus(claimId, req);
        auditLogService.log(adminId, "APPROVE_CLAIM", "Claim", claimId, remarks);
        evictDashboardCache();
        return updated;
    }

    public ClaimDTO rejectClaim(Long adminId, Long claimId, String remarks) {
        ClaimStatusUpdateRequest req = new ClaimStatusUpdateRequest("REJECTED", remarks);
        ClaimDTO updated = claimFeignClient.updateClaimStatus(claimId, req);
        auditLogService.log(adminId, "REJECT_CLAIM", "Claim", claimId, remarks);
        evictDashboardCache();
        return updated;
    }

    public ClaimDTO markUnderReview(Long adminId, Long claimId) {
        ClaimDTO claim = claimFeignClient.getClaimById(claimId);
        if ("SUBMITTED".equals(claim.getStatus())) {
            ClaimStatusUpdateRequest req =
                    new ClaimStatusUpdateRequest("UNDER_REVIEW", "Claim moved to under review");
            ClaimDTO updated = claimFeignClient.updateClaimStatus(claimId, req);
            auditLogService.log(adminId, "MARK_UNDER_REVIEW", "Claim", claimId,
                    "Claim moved to under review");
            return updated;
        }
        auditLogService.log(adminId, "MARK_UNDER_REVIEW", "Claim", claimId,
                "Claim already under review");
        return claim;
    }

    // ══════════════════════════════════════════════════════════
    // POLICY MANAGEMENT
    // ══════════════════════════════════════════════════════════

    public List<PolicyDTO> getAllPolicies() {
        return policyFeignClient.getAllPolicies();
    }

    public PolicyDTO getPolicyById(Long policyId) {
        return policyFeignClient.getPolicyById(policyId);
    }

    public PolicyDTO cancelPolicy(Long adminId, Long policyId, String reason) {
        PolicyStatusUpdateRequest req = new PolicyStatusUpdateRequest("CANCELLED", reason);
        PolicyDTO updated = policyFeignClient.updatePolicyStatus(policyId, req);
        auditLogService.log(adminId, "CANCEL_POLICY", "Policy", policyId, reason);
        evictDashboardCache();
        return updated;
    }

    // ══════════════════════════════════════════════════════════
    // PAYMENT MANAGEMENT
    // ══════════════════════════════════════════════════════════

    public List<PaymentDTO> getAllPayments() {
        return paymentFeignClient.getAllPayments();
    }

    public PaymentDTO getPaymentById(Long paymentId) {
        return paymentFeignClient.getPaymentById(paymentId);
    }

    public List<PaymentDTO> getPaymentsByCustomer(Long customerId) {
        return paymentFeignClient.getPaymentsByCustomer(customerId);
    }

    public List<PaymentDTO> getPaymentsByPolicy(Long policyId) {
        return paymentFeignClient.getPaymentsByPolicy(policyId);
    }

    // ══════════════════════════════════════════════════════════
    // USER MANAGEMENT
    // ══════════════════════════════════════════════════════════

    public List<UserDTO> getAllUsers() {
        return userFeignClient.getAllUsers();
    }

    public UserDTO getUserById(Long userId) {
        return userFeignClient.getUserById(userId);
    }

    public UserDTO deactivateUser(Long adminId, Long userId) {
        UserDTO updated = userFeignClient.deactivateUser(userId);
        auditLogService.log(adminId, "DEACTIVATE_USER", "User", userId,
                "User account deactivated by admin");
        return updated;
    }

    // Get all policies for a specific customer — admin user detail view
    public List<PolicyDTO> getPoliciesByUser(Long userId) {
        return policyFeignClient.getAllPolicies().stream()
                .filter(p -> userId.equals(p.getCustomerId()))
                .collect(Collectors.toList());
    }

    // Get all claims for a specific customer — admin user detail view
    public List<ClaimDTO> getClaimsByUser(Long userId) {
        return claimFeignClient.getClaimsByUser(userId);
    }

    // ══════════════════════════════════════════════════════════
    // AUDIT LOGS
    // ══════════════════════════════════════════════════════════

    public List<AuditLogDTO> getRecentActivity(int limit) {
        return auditLogService.getRecentLogs(limit);
    }

    public List<AuditLogDTO> getEntityHistory(String entity, Long id) {
        return auditLogService.getLogsByEntityAndId(entity, id);
    }

    public List<AuditLogDTO> getLogsByAdmin(Long adminId) {
        return auditLogService.getLogsByAdmin(adminId);
    }

    // ══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    private long count(List<ClaimDTO> claims, String status) {
        return claims.stream().filter(c -> status.equals(c.getStatus())).count();
    }
}