package com.smartSure.claimService.controller;

import com.smartSure.claimService.dto.*;
import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.service.ClaimService;
import com.smartSure.claimService.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * ClaimController — Digital-First API.
 *
 * New endpoints added for digital flow:
 *   PUT /api/claims/{id}/consent  — customer gives digital consent
 *
 * Removed:
 *   POST /api/claims/{id}/upload/claim-form — no longer needed (auto-generated)
 *
 * Simplified:
 *   Evidence is the only required manual upload
 *   Aadhaar upload is optional (kept for backward compatibility)
 */
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    // ─── CREATE — Customer fills digital form ─────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> createClaim(
            @Valid @RequestBody ClaimRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.createClaim(request, userId));
    }

    // ─── DIGITAL CONSENT — replaces physical signature ────────────────────────
    @PutMapping("/{id}/consent")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> giveConsent(
            @PathVariable Long id,
            @RequestBody ConsentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.getCurrentUserId();
        // Capture IP address for legal consent record
        String ipAddress = getClientIp(httpRequest);
        return ResponseEntity.ok(
                claimService.giveConsent(id, userId, request.isConsentGiven(), ipAddress));
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> submitClaim(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.submitClaim(id, userId));
    }

    // ─── READ — Customer ──────────────────────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ClaimResponse>> getMyClaims() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.getMyClaims(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        return ResponseEntity.ok(claimService.getClaimById(id, userId, isAdmin));
    }

    @GetMapping("/{id}/policy")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PolicyDTO> getPolicyForClaim(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        return ResponseEntity.ok(claimService.getPolicyForClaim(id, userId, isAdmin));
    }

    // ─── READ — Admin ─────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }
    @GetMapping("/customer/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getClaimsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(claimService.getMyClaims(userId));
    }

    @GetMapping("/under-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllUnderReviewClaims() {
        return ResponseEntity.ok(claimService.getAllUnderReviewClaims());
    }

    // ─── INTERNAL ─────────────────────────────────────────────────────────────
    @GetMapping("/internal/total-approved/{policyId}")
    public ResponseEntity<java.math.BigDecimal> getTotalApprovedClaimsAmount(@PathVariable Long policyId) {
        // Internal endpoint called via FeignClient; no user auth required if secured downstream
        return ResponseEntity.ok(claimService.getApprovedClaimTotal(policyId));
    }

    // ─── ADMIN STATUS UPDATE — with mandatory remarks on rejection ────────────
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClaimResponse> moveToStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                claimService.moveToStatus(id, request.getNextStatus(), request.getRemarks()));
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        claimService.deleteClaim(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── FILE UPLOADS ─────────────────────────────────────────────────────────

    // Evidence is the ONLY required manual upload in the digital flow
    @PostMapping(value = "/{id}/upload/evidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadEvidence(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.uploadEvidence(id, file, userId));
    }

    // Aadhaar — optional in digital flow (kept for backward compatibility)
    @PostMapping(value = "/{id}/upload/aadhaar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadAadhaarCard(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.uploadAadhaarCard(id, file, userId));
    }

    // ─── FILE DOWNLOADS ───────────────────────────────────────────────────────

    // Download auto-generated claim form PDF
    @GetMapping("/{id}/download/claim-form")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadClaimForm(@PathVariable Long id) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        Claim claim = claimService.getClaimEntity(id, userId, isAdmin);
        byte[] data = claimService.downloadClaimForm(id, userId, isAdmin);
        return buildFileResponse(data,
                claim.getClaimForm().getFileName(),
                claim.getClaimForm().getFileType());
    }

    @GetMapping("/{id}/download/evidence")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        Claim claim = claimService.getClaimEntity(id, userId, isAdmin);
        byte[] data = claimService.downloadEvidence(id, userId, isAdmin);
        return buildFileResponse(data,
                claim.getEvidences().getFileName(),
                claim.getEvidences().getFileType());
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildFileResponse(byte[] data,
                                                     String fileName,
                                                     String fileType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(data);
    }

    /**
     * Extracts real client IP — handles reverse proxy / load balancer headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}