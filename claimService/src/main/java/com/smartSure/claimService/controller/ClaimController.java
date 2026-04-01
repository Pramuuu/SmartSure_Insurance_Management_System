package com.smartSure.claimService.controller;

import com.smartSure.claimService.dto.ClaimRequest;
import com.smartSure.claimService.dto.ClaimResponse;
import com.smartSure.claimService.dto.PolicyDTO;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.service.ClaimService;
import com.smartSure.claimService.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    // POST /api/claims
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody ClaimRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.createClaim(request, userId));
    }

    // GET /api/claims/my
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ClaimResponse>> getMyClaims() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.getMyClaims(userId));
    }

    // GET /api/claims/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        return ResponseEntity.ok(claimService.getClaimById(id, userId, isAdmin));
    }

    // GET /api/claims
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    // GET /api/claims/under-review
    @GetMapping("/under-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllUnderReviewClaims() {
        return ResponseEntity.ok(claimService.getAllUnderReviewClaims());
    }

    // GET /api/claims/{id}/policy
    @GetMapping("/{id}/policy")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PolicyDTO> getPolicyForClaim(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        return ResponseEntity.ok(claimService.getPolicyForClaim(id, userId, isAdmin));
    }

    // DELETE /api/claims/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        claimService.deleteClaim(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> submitClaim(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.submitClaim(id, userId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClaimResponse> moveToStatus(
            @PathVariable Long id,
            @RequestParam Status next) {
        return ResponseEntity.ok(claimService.moveToStatus(id, next));
    }

    @PostMapping(value = "/{id}/upload/claim-form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadClaimForm(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.uploadClaimForm(id, file, userId));
    }

    @PostMapping(value = "/{id}/upload/aadhaar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadAadhaarCard(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.uploadAadhaarCard(id, file, userId));
    }

    @PostMapping(value = "/{id}/upload/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> uploadEvidence(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(claimService.uploadEvidence(id, file, userId));
    }

    @GetMapping("/{id}/download/claim-form")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadClaimForm(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        return buildFileResponse(claimService.downloadClaimForm(id, userId, isAdmin));
    }

    @GetMapping("/{id}/download/aadhaar")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadAadhaarCard(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        return buildFileResponse(claimService.downloadAadhaarCard(id, userId, isAdmin));
    }

    @GetMapping("/{id}/download/evidence")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole());
        return buildFileResponse(claimService.downloadEvidence(id, userId, isAdmin));
    }

    private ResponseEntity<byte[]> buildFileResponse(FileData file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .body(file.getData());
    }
}