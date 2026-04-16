package com.smartSure.claimService.dto;

import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for Claim — includes all digital form fields and status flags.
 * Frontend uses this to render the claim detail page and track progress.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimResponse {

    private Long id;
    private Long policyId;
    private Long userId;
    private BigDecimal amount;
    private Status status;

    // ── Incident details ──────────────────────────────────────────────────────
    private String description;
    private LocalDate incidentDate;
    private String incidentType;
    private String incidentLocation;
    private String hospitalName;
    private String treatmentType;
    private String vehicleNumber;
    private String garageRepairShop;
    private String policeReportNumber;
    private String witnessName;
    private String witnessContact;

    // ── Document and consent status ───────────────────────────────────────────
    // true = PDF was auto-generated from form data
    private boolean pdfGenerated;
    // true = evidence photo/document has been uploaded
    private boolean evidenceUploaded;
    // true = aadhaar uploaded (optional in digital flow)
    private boolean aadhaarUploaded;
    // true = customer gave digital consent
    private boolean consentGiven;
    private LocalDateTime consentTimestamp;

    // ── Admin decision ────────────────────────────────────────────────────────
    private String adminRemarks;
    private LocalDateTime reviewedAt;

    // ── Auto-approval flag ────────────────────────────────────────────────────
    private boolean autoApproved;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private LocalDateTime timeOfCreation;
    private LocalDateTime lastUpdatedAt;
}