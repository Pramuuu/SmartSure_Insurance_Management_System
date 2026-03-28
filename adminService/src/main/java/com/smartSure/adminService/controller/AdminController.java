package com.smartSure.adminService.controller;

import com.smartSure.adminService.dto.AuditLogDTO;
import com.smartSure.adminService.dto.ClaimDTO;
import com.smartSure.adminService.dto.ClaimStatusUpdateRequest;
import com.smartSure.adminService.dto.PolicyDTO;
import com.smartSure.adminService.dto.UserDTO;
import com.smartSure.adminService.entity.AuditLog;
import com.smartSure.adminService.service.AdminService;
import com.smartSure.adminService.service.AuditLogService;
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
@Tag(name = "Admin Controller", description = "Admin operations — claim review, policy management, user management, audit logs")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    // ==================== CLAIM MANAGEMENT ====================

    @GetMapping("/claims")
    @Operation(summary = "Get all claims")
    public ResponseEntity<List<ClaimDTO>> getAllClaims() {
        return ResponseEntity.ok(adminService.getAllClaims());
    }

    @GetMapping("/claims/under-review")
    @Operation(summary = "Get all claims currently under review")
    public ResponseEntity<List<ClaimDTO>> getUnderReviewClaims() {
        return ResponseEntity.ok(adminService.getUnderReviewClaims());
    }

    @GetMapping("/claims/{claimId}")
    @Operation(summary = "Get a single claim by ID")
    public ResponseEntity<ClaimDTO> getClaimById(@PathVariable Long claimId) {
        return ResponseEntity.ok(adminService.getClaimById(claimId));
    }

    // Admin starts reviewing a claim — moves it from SUBMITTED → UNDER_REVIEW
    @PutMapping("/claims/{claimId}/review")
    @Operation(summary = "Mark claim as under review")
    public ResponseEntity<ClaimDTO> markUnderReview(
            @PathVariable Long claimId,
            @RequestHeader("X-Admin-Id") Long adminId) {
        return ResponseEntity.ok(adminService.markUnderReview(adminId, claimId));
    }

    // Admin approves a claim — moves it to APPROVED and logs the action
    @PutMapping("/claims/{claimId}/approve")
    @Operation(summary = "Approve a claim")
    public ResponseEntity<ClaimDTO> approveClaim(
            @PathVariable Long claimId,
            @RequestHeader("X-Admin-Id") Long adminId,
            @RequestBody ClaimStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.approveClaim(adminId, claimId, request.getRemarks()));
    }

    // Admin rejects a claim — moves it to REJECTED and logs the action
    @PutMapping("/claims/{claimId}/reject")
    @Operation(summary = "Reject a claim")
    public ResponseEntity<ClaimDTO> rejectClaim(
            @PathVariable Long claimId,
            @RequestHeader("X-Admin-Id") Long adminId,
            @RequestBody ClaimStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.rejectClaim(adminId, claimId, request.getRemarks()));
    }

    // ==================== POLICY MANAGEMENT ====================

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

    // Admin cancels a policy — calls policyService and logs the action
    @PutMapping("/policies/{policyId}/cancel")
    @Operation(summary = "Cancel a policy")
    public ResponseEntity<PolicyDTO> cancelPolicy(
            @PathVariable Long policyId,
            @RequestHeader("X-Admin-Id") Long adminId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(adminService.cancelPolicy(adminId, policyId, reason));
    }

    // ==================== USER MANAGEMENT ====================

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

    // ==================== AUDIT LOGS ====================

    @GetMapping("/audit-logs")
    @Operation(summary = "Get all audit logs")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/audit-logs/recent")
    @Operation(summary = "Get recent admin activity (last N entries)")
    public ResponseEntity<List<AuditLog>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminService.getRecentActivity(limit));
    }

    @GetMapping("/audit-logs/{entity}/{id}")
    @Operation(summary = "Get full audit history for a specific Claim or Policy")
    public ResponseEntity<List<AuditLog>> getEntityHistory(
            @PathVariable String entity,
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEntityHistory(entity, id));
    }

    @GetMapping("/audit-logs/range")
    @Operation(summary = "Get audit logs within a date range")
    public ResponseEntity<List<AuditLog>> getLogsByDateRange(
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
