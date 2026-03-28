package com.smartSure.claimService.controller;

import com.smartSure.claimService.dto.ClaimRequest;
import com.smartSure.claimService.dto.ClaimResponse;
import com.smartSure.claimService.dto.PolicyDTO;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    // POST /api/claims
    // Called by Policy Service via OpenFeign — creates a DRAFT claim.
    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@RequestBody ClaimRequest request) {
        return ResponseEntity.ok(claimService.createClaim(request));
    }

    // GET /api/claims/{id}
    // Customer tracks their own claim.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    // GET /api/claims
    // Admin views all claims.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    // GET /api/claims/under-review
    // Admin review queue.
    @GetMapping("/under-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllUnderReviewClaims() {
        return ResponseEntity.ok(claimService.getAllUnderReviewClaims());
    }

    // GET /api/claims/{id}/policy
    // Fetch policy details for a claim via OpenFeign.
    @GetMapping("/{id}/policy")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PolicyDTO> getPolicyForClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getPolicyForClaim(id));
    }

    // DELETE /api/claims/{id}
    // Only DRAFT claims can be deleted.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // SUBMIT  (Customer explicitly submits after uploading all 3 docs)
    // ─────────────────────────────────────────────

    /**
     * PUT /api/claims/{id}/submit
     * Customer manually submits the claim after uploading all 3 documents.
     * Service validates all 3 docs are present — throws 400 if any is missing.
     * On success: DRAFT → SUBMITTED → UNDER_REVIEW in a single call.
     */
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> submitClaim(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.submitClaim(id));
    }

    // ─────────────────────────────────────────────
    // STATUS TRANSITION  (Admin only)
    // ─────────────────────────────────────────────

    // PUT /api/claims/{id}/status?next=APPROVED
    // Admin moves claim to APPROVED or REJECTED.
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClaimResponse> moveToStatus(
            @PathVariable Long id,
            @RequestParam Status next) {
        return ResponseEntity.ok(claimService.moveToStatus(id, next));
    }

    // ─────────────────────────────────────────────
    // DOCUMENT UPLOAD  (Customer — no status change on upload)
    // ─────────────────────────────────────────────

    // POST /api/claims/{id}/upload/claim-form
    @PostMapping(value = "/{id}/upload/claim-form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadClaimForm(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadClaimForm(id, file));
    }

    // POST /api/claims/{id}/upload/aadhaar
    @PostMapping(value = "/{id}/upload/aadhaar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadAadhaarCard(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadAadhaarCard(id, file));
    }

    // POST /api/claims/{id}/upload/evidence
    @PostMapping(value = "/{id}/upload/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadEvidence(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(claimService.uploadEvidence(id, file));
    }

    // ─────────────────────────────────────────────
    // DOCUMENT DOWNLOAD
    // ─────────────────────────────────────────────

    // GET /api/claims/{id}/download/claim-form
    @GetMapping("/{id}/download/claim-form")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadClaimForm(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadClaimForm(id));
    }

    // GET /api/claims/{id}/download/aadhaar
    @GetMapping("/{id}/download/aadhaar")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadAadhaarCard(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadAadhaarCard(id));
    }

    // GET /api/claims/{id}/download/evidence
    @GetMapping("/{id}/download/evidence")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id) {
        return buildFileResponse(claimService.downloadEvidence(id));
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────

    private ResponseEntity<byte[]> buildFileResponse(FileData file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .body(file.getData());
    }
}