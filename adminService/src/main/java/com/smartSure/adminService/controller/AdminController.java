package com.smartSure.adminService.controller;

import com.smartSure.adminService.dto.*;
import com.smartSure.adminService.service.AdminService;
import com.smartSure.adminService.service.AuditLogService;
import com.smartSure.adminService.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Controller", description = "Admin operations — claims, policies, payments, users, audit logs")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    // ══════════════════════════════════════════════════════════
    // DASHBOARD
    // ══════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard metrics — cached 3 min")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        return ResponseEntity.ok(adminService.getDashboardMetrics());
    }

    @GetMapping("/dashboard/policy-stats")
    @Operation(summary = "Policy status breakdown — ACTIVE/EXPIRED/CANCELLED counts and amounts")
    public ResponseEntity<PolicyStatsResponse> getPolicyStats() {
        return ResponseEntity.ok(adminService.getPolicyStats());
    }

    @GetMapping("/dashboard/claim-stats")
    @Operation(summary = "Claim status breakdown with amounts — approved total, pending total")
    public ResponseEntity<ClaimStatsResponse> getClaimStats() {
        return ResponseEntity.ok(adminService.getClaimStats());
    }

    // ══════════════════════════════════════════════════════════
    // CLAIM MANAGEMENT
    // ══════════════════════════════════════════════════════════

    @GetMapping("/claims")
    @Operation(summary = "Get all claims")
    public ResponseEntity<List<ClaimDTO>> getAllClaims() {
        return ResponseEntity.ok(adminService.getAllClaims());
    }

    @GetMapping("/claims/pending")
    @Operation(summary = "Admin work queue — SUBMITTED + UNDER_REVIEW claims")
    public ResponseEntity<List<ClaimDTO>> getPendingClaims() {
        return ResponseEntity.ok(adminService.getPendingClaims());
    }

    @GetMapping("/claims/under-review")
    @Operation(summary = "Get claims currently under review")
    public ResponseEntity<List<ClaimDTO>> getUnderReviewClaims() {
        return ResponseEntity.ok(adminService.getUnderReviewClaims());
    }

    @GetMapping("/claims/{claimId}")
    @Operation(summary = "Get a single claim by ID")
    public ResponseEntity<ClaimDTO> getClaimById(@PathVariable Long claimId) {
        return ResponseEntity.ok(adminService.getClaimById(claimId));
    }

    @PutMapping("/claims/{claimId}/review")
    @Operation(summary = "Mark claim as under review — SUBMITTED → UNDER_REVIEW")
    public ResponseEntity<ClaimDTO> markUnderReview(@PathVariable Long claimId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.markUnderReview(adminId, claimId));
    }

    @PutMapping("/claims/{claimId}/approve")
    @Operation(summary = "Approve a claim")
    public ResponseEntity<ClaimDTO> approveClaim(
            @PathVariable Long claimId,
            @RequestBody ClaimStatusUpdateRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.approveClaim(adminId, claimId, request.getRemarks()));
    }

    @PutMapping("/claims/{claimId}/reject")
    @Operation(summary = "Reject a claim — remarks mandatory (IRDAI compliance)")
    public ResponseEntity<ClaimDTO> rejectClaim(
            @PathVariable Long claimId,
            @RequestBody ClaimStatusUpdateRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.rejectClaim(adminId, claimId, request.getRemarks()));
    }

    // ══════════════════════════════════════════════════════════
    // POLICY MANAGEMENT
    // ══════════════════════════════════════════════════════════

    @GetMapping("/policies")
    @Operation(summary = "Get all policies")
    public ResponseEntity<List<PolicyDTO>> getAllPolicies() {
        return ResponseEntity.ok(adminService.getAllPolicies());
    }

    @GetMapping("/policies/{policyId}")
    @Operation(summary = "Get a single policy by ID")
    public ResponseEntity<PolicyDTO> getPolicyById(@PathVariable Long policyId) {
        return ResponseEntity.ok(adminService.getPolicyById(policyId));
    }

    @PutMapping("/policies/{policyId}/cancel")
    @Operation(summary = "Cancel a policy")
    public ResponseEntity<PolicyDTO> cancelPolicy(
            @PathVariable Long policyId,
            @RequestParam(required = false) String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.cancelPolicy(adminId, policyId, reason));
    }

    // ══════════════════════════════════════════════════════════
    // PAYMENT MANAGEMENT — previously no payment visibility
    // ══════════════════════════════════════════════════════════

    @GetMapping("/payments")
    @Operation(summary = "Get all payments — includes premium payments and claim payouts")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        return ResponseEntity.ok(adminService.getAllPayments());
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "Get a single payment by ID")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(adminService.getPaymentById(paymentId));
    }

    @GetMapping("/payments/policy/{policyId}")
    @Operation(summary = "Get all payments for a specific policy")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByPolicy(@PathVariable Long policyId) {
        return ResponseEntity.ok(adminService.getPaymentsByPolicy(policyId));
    }

    // ══════════════════════════════════════════════════════════
    // USER MANAGEMENT
    // ══════════════════════════════════════════════════════════

    @GetMapping("/users")
    @Operation(summary = "Get all registered users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get a single user by ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @GetMapping("/users/{userId}/policies")
    @Operation(summary = "Get all policies for a specific customer")
    public ResponseEntity<List<PolicyDTO>> getUserPolicies(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getPoliciesByUser(userId));
    }

    @GetMapping("/users/{userId}/claims")
    @Operation(summary = "Get all claims for a specific customer")
    public ResponseEntity<List<ClaimDTO>> getUserClaims(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getClaimsByUser(userId));
    }

    @GetMapping("/users/{userId}/payments")
    @Operation(summary = "Get all payments for a specific customer")
    public ResponseEntity<List<PaymentDTO>> getUserPayments(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getPaymentsByCustomer(userId));
    }

    @PutMapping("/users/{userId}/deactivate")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<UserDTO> deactivateUser(@PathVariable Long userId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.deactivateUser(adminId, userId));
    }

    // ══════════════════════════════════════════════════════════
    // AUDIT LOGS
    // ══════════════════════════════════════════════════════════

    @GetMapping("/audit-logs")
    @Operation(summary = "Get all audit logs")
    public ResponseEntity<List<AuditLogDTO>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/audit-logs/recent")
    @Operation(summary = "Get recent admin activity")
    public ResponseEntity<List<AuditLogDTO>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditLogService.getRecentLogs(limit));
    }

    @GetMapping("/audit-logs/admin/{adminId}")
    @Operation(summary = "Get all actions performed by a specific admin")
    public ResponseEntity<List<AuditLogDTO>> getLogsByAdmin(@PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getLogsByAdmin(adminId));
    }

    @GetMapping("/audit-logs/{entity}/{id}")
    @Operation(summary = "Get full audit history for a specific Claim or Policy")
    public ResponseEntity<List<AuditLogDTO>> getEntityHistory(
            @PathVariable String entity,
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEntityHistory(entity, id));
    }

    @GetMapping("/audit-logs/range")
    @Operation(summary = "Get audit logs within a date range — format: 2024-01-01T00:00:00")
    public ResponseEntity<List<AuditLogDTO>> getLogsByDateRange(
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(
                auditLogService.getLogsByDateRange(
                        LocalDateTime.parse(from),
                        LocalDateTime.parse(to)
                )
        );
    }
}