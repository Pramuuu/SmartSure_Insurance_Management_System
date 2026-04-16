package com.smartSure.claimService.service;

import com.smartSure.claimService.client.PolicyClient;
import com.smartSure.claimService.client.UserClient;
import com.smartSure.claimService.dto.*;
import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.exception.ClaimDeletionNotAllowedException;
import com.smartSure.claimService.exception.ClaimNotFoundException;
import com.smartSure.claimService.exception.DocumentNotUploadedException;
import com.smartSure.claimService.messaging.ClaimDecisionEvent;
import com.smartSure.claimService.messaging.RabbitMQConfig;
import com.smartSure.claimService.repository.ClaimRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClaimService — Digital-First architecture.
 *
 * New flow vs old flow:
 *
 * OLD: Customer downloads PDF → fills manually → scans → uploads 3 files → submits
 * NEW: Customer fills digital form → PDF auto-generated → uploads evidence only
 *      → gives digital consent → submits
 *
 * Key improvements:
 *  - Auto PDF generation from form data (PdfGenerationService)
 *  - Digital consent with timestamp + IP (IT Act 2000 compliant)
 *  - Auto-approval for low-value claims (configurable threshold)
 *  - Duplicate claim detection (same policy + open claim)
 *  - Actual claim amount validation against policy coverage
 *  - Mandatory admin remarks on rejection (IRDAI compliance)
 *  - Files stored on disk as paths — no LONGBLOB in MySQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyClient policyClient;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;
    private final FileStorageService fileStorageService;
    private final PdfGenerationService pdfGenerationService;

    // Claims below this amount are auto-approved (configurable)
    @Value("${claim.auto.approval.threshold:10000}")
    private BigDecimal autoApprovalThreshold;

    // ─── CREATE — Digital Form Submission ────────────────────────────────────

    @Transactional
    public ClaimResponse createClaim(ClaimRequest request, Long userId) {

        // Step 1: Validate policy ownership
        PolicyDTO policy = policyClient.getPolicyById(request.getPolicyId());
        if (!policy.getCustomerId().equals(userId)) {
            throw new RuntimeException("Policy does not belong to you");
        }

        // Step 2: Validate claim amount against coverage
        if (request.getClaimAmount().compareTo(policy.getCoverageAmount()) > 0) {
            throw new RuntimeException(
                    "Claim amount ₹" + request.getClaimAmount()
                            + " cannot exceed policy coverage ₹" + policy.getCoverageAmount());
        }

        // Step 3: Prevent duplicate active claims
        boolean existingOpenClaim = claimRepository
                .existsByUserIdAndPolicyIdAndStatusNotIn(
                        userId,
                        request.getPolicyId(),
                        List.of(Status.REJECTED, Status.CLOSED)
                );
        if (existingOpenClaim) {
            throw new RuntimeException(
                    "You already have an active claim for this policy. " +
                            "Please wait for your existing claim to be resolved.");
        }

        // Step 4: Build claim entity from digital form data
        Claim claim = new Claim();
        claim.setPolicyId(request.getPolicyId());
        claim.setUserId(userId);
        claim.setAmount(request.getClaimAmount());
        claim.setDescription(request.getDescription());
        claim.setIncidentType(request.getIncidentType());
        claim.setIncidentLocation(request.getIncidentLocation());

        if (request.getIncidentDate() != null && !request.getIncidentDate().isBlank()) {
            claim.setIncidentDate(LocalDate.parse(request.getIncidentDate()));
        }

        // Conditional fields by incident type
        if (request.getIncidentType() != null) {
            switch (request.getIncidentType()) {
                case HEALTH -> {
                    claim.setHospitalName(request.getHospitalName());
                    claim.setTreatmentType(request.getTreatmentType());
                }
                case ACCIDENT, THEFT -> {
                    claim.setVehicleNumber(request.getVehicleNumber());
                    claim.setGarageRepairShop(request.getGarageRepairShop());
                    claim.setPoliceReportNumber(request.getPoliceReportNumber());
                    claim.setWitnessName(request.getWitnessName());
                    claim.setWitnessContact(request.getWitnessContact());
                }
                default -> {}
            }
        }

        // Step 5: Save claim first to get the ID
        Claim saved = claimRepository.save(claim);

        // Step 6: Auto-generate PDF from form data — no manual form needed
        try {
            String pdfPath = pdfGenerationService.generateClaimFormPdf(saved);
            saved.setClaimForm(new FileData(
                    "claim_form_" + saved.getId() + ".pdf",
                    "application/pdf",
                    pdfPath
            ));
            saved.setPdfGenerated(true);
            saved = claimRepository.save(saved);
            log.info("Auto-generated claim form PDF — claimId={}", saved.getId());
        } catch (Exception e) {
            // PDF generation failure should not block claim creation
            log.error("PDF generation failed for claimId={}: {}", saved.getId(), e.getMessage());
        }

        log.info("Claim created (digital form) — claimId={}, userId={}, type={}, amount={}",
                saved.getId(), userId, request.getIncidentType(), request.getClaimAmount());

        return toResponse(saved);
    }

    // ─── DIGITAL CONSENT ─────────────────────────────────────────────────────

    @Transactional
    public ClaimResponse giveConsent(Long claimId, Long userId,
                                     boolean consentGiven, String ipAddress) {
        Claim claim = findOrThrow(claimId, userId, false);

        if (claim.getStatus() != Status.DRAFT) {
            throw new IllegalStateException("Consent can only be given for DRAFT claims");
        }
        if (!consentGiven) {
            throw new IllegalArgumentException(
                    "You must accept the declaration to proceed");
        }

        claim.setConsentGiven(true);
        claim.setConsentTimestamp(LocalDateTime.now());
        claim.setConsentIpAddress(ipAddress);

        log.info("Digital consent recorded — claimId={}, userId={}, ip={}",
                claimId, userId, ipAddress);

        return toResponse(claimRepository.save(claim));
    }

    // ─── SUBMIT ───────────────────────────────────────────────────────────────

    @Transactional
    public ClaimResponse submitClaim(Long claimId, Long userId) {
        Claim claim = findOrThrow(claimId, userId, false);

        if (claim.getStatus() != Status.DRAFT) {
            throw new IllegalStateException(
                    "Claim " + claimId + " cannot be submitted — not in DRAFT status.");
        }

        // Digital flow validation — evidence + consent required
        // PDF is auto-generated so no claimForm check needed
        if (claim.getEvidences() == null) {
            throw new DocumentNotUploadedException("Evidence", claimId);
        }
        if (!Boolean.TRUE.equals(claim.getConsentGiven())) {
            throw new IllegalStateException(
                    "Digital consent is required before submitting the claim");
        }

        // Check for auto-approval — low value claims processed instantly
        if (claim.getAmount().compareTo(autoApprovalThreshold) <= 0) {
            claim.setStatus(Status.APPROVED);
            claim.setAutoApproved(true);
            claim.setAdminRemarks("Auto-approved: claim amount below threshold of ₹"
                    + autoApprovalThreshold);
            claim.setReviewedAt(LocalDateTime.now());
            Claim saved = claimRepository.save(claim);
            publishDecisionEvent(saved, Status.APPROVED);
            log.info("Claim AUTO-APPROVED — claimId={}, amount={}", claimId, claim.getAmount());
            return toResponse(saved);
        }

        // Normal flow — move to SUBMITTED for admin review
        claim.setStatus(claim.getStatus().moveTo(Status.SUBMITTED));

        log.info("Claim submitted — claimId={}, userId={}", claimId, userId);
        return toResponse(claimRepository.save(claim));
    }

    // ─── ADMIN STATUS UPDATE ──────────────────────────────────────────────────

    @Transactional
    public ClaimResponse moveToStatus(Long claimId, Status nextStatus, String remarks) {

        Claim claim = findOrThrow(claimId, null, true);

        // IRDAI compliance — rejection must have a reason
        if (nextStatus == Status.REJECTED &&
                (remarks == null || remarks.isBlank())) {
            throw new IllegalArgumentException(
                    "Remarks are mandatory when rejecting a claim (IRDAI regulation)");
        }

        claim.setStatus(claim.getStatus().moveTo(nextStatus));

        if (remarks != null && !remarks.isBlank()) {
            claim.setAdminRemarks(remarks);
        }
        claim.setReviewedAt(LocalDateTime.now());

        Claim saved = claimRepository.save(claim);

        if (nextStatus == Status.APPROVED || nextStatus == Status.REJECTED) {
            publishDecisionEvent(saved, nextStatus);
        }

        log.info("Claim status updated — claimId={}, newStatus={}", claimId, nextStatus);
        return toResponse(saved);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    public BigDecimal getApprovedClaimTotal(Long policyId) {
        return claimRepository.sumApprovedClaimsByPolicyId(policyId);
    }

    public ClaimResponse getClaimById(Long claimId, Long userId, boolean isAdmin) {
        return toResponse(findOrThrow(claimId, userId, isAdmin));
    }

    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getMyClaims(Long userId) {
        return claimRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getAllUnderReviewClaims() {
        return claimRepository
                .findByStatusIn(List.of(Status.SUBMITTED, Status.UNDER_REVIEW))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PolicyDTO getPolicyForClaim(Long claimId, Long userId, boolean isAdmin) {
        return policyClient.getPolicyById(
                findOrThrow(claimId, userId, isAdmin).getPolicyId());
    }

    public Claim getClaimEntity(Long claimId, Long userId, boolean isAdmin) {
        return findOrThrow(claimId, userId, isAdmin);
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteClaim(Long claimId, Long userId) {
        Claim claim = findOrThrow(claimId, userId, false);
        if (claim.getStatus() != Status.DRAFT) {
            throw new ClaimDeletionNotAllowedException(claimId);
        }
        claimRepository.deleteById(claimId);
        fileStorageService.deleteClaimFiles(claimId);
    }

    // ─── FILE UPLOADS ─────────────────────────────────────────────────────────

    @Transactional
    public ClaimResponse uploadEvidence(Long claimId, MultipartFile file,
                                        Long userId) throws IOException {
        Claim claim = findOrThrow(claimId, userId, false);
        String path = fileStorageService.saveFile(file, claimId, "evidence");
        claim.setEvidences(new FileData(
                file.getOriginalFilename(), file.getContentType(), path));
        log.info("Evidence uploaded — claimId={}", claimId);
        return toResponse(claimRepository.save(claim));
    }

    // Kept for backward compatibility — optional in digital flow
    @Transactional
    public ClaimResponse uploadAadhaarCard(Long claimId, MultipartFile file,
                                           Long userId) throws IOException {
        Claim claim = findOrThrow(claimId, userId, false);
        String path = fileStorageService.saveFile(file, claimId, "aadhaar");
        claim.setAadhaarCard(new FileData(
                file.getOriginalFilename(), file.getContentType(), path));
        return toResponse(claimRepository.save(claim));
    }

    // ─── FILE DOWNLOADS ───────────────────────────────────────────────────────

    public byte[] downloadClaimForm(Long claimId, Long userId,
                                    boolean isAdmin) throws IOException {
        Claim claim = findOrThrow(claimId, userId, isAdmin);
        if (claim.getClaimForm() == null) {
            throw new DocumentNotUploadedException("Claim form", claimId);
        }
        
        try {
            // Auto-heal legacy corrupted PDFs by regenerating them on the fly
            String newPdfPath = pdfGenerationService.generateClaimFormPdf(claim);
            claim.getClaimForm().setFilePath(newPdfPath);
            claimRepository.save(claim);
        } catch (Exception e) {
            log.error("Failed to auto-heal PDF for claim {}", claimId, e);
        }
        
        return fileStorageService.readFile(claim.getClaimForm().getFilePath());
    }

    public byte[] downloadEvidence(Long claimId, Long userId,
                                   boolean isAdmin) throws IOException {
        Claim claim = findOrThrow(claimId, userId, isAdmin);
        if (claim.getEvidences() == null) {
            throw new DocumentNotUploadedException("Evidence", claimId);
        }
        return fileStorageService.readFile(claim.getEvidences().getFilePath());
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private void publishDecisionEvent(Claim claim, Status decision) {
        try {
            PolicyDTO policy = policyClient.getPolicyById(claim.getPolicyId());
            UserResponseDto user = userClient.getUserById(policy.getCustomerId());

            ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                    .claimId(claim.getId())
                    .policyId(claim.getPolicyId())
                    .customerId(claim.getUserId())
                    .decision(decision.name())
                    .amount(claim.getAmount())
                    .customerEmail(user.getEmail())
                    .customerName(user.getName())
                    .remarks(claim.getAdminRemarks())
                    .decidedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.CLAIM_DECISION_KEY,
                    event
            );
            log.info("ClaimDecisionEvent published — claimId={}, decision={}",
                    claim.getId(), decision);

        } catch (Exception e) {
            log.error("Failed to publish ClaimDecisionEvent for claim {}: {}",
                    claim.getId(), e.getMessage());
        }
    }

    private Claim findOrThrow(Long claimId, Long userId, boolean isAdmin) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));
        if (!isAdmin && userId != null && !claim.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only access your own claims");
        }
        return claim;
    }

    private ClaimResponse toResponse(Claim claim) {
        return ClaimResponse.builder()
                .id(claim.getId())
                .policyId(claim.getPolicyId())
                .userId(claim.getUserId())
                .amount(claim.getAmount())
                .status(claim.getStatus())
                .description(claim.getDescription())
                .incidentDate(claim.getIncidentDate())
                .incidentType(claim.getIncidentType() != null
                        ? claim.getIncidentType().name() : null)
                .incidentLocation(claim.getIncidentLocation())
                .hospitalName(claim.getHospitalName())
                .treatmentType(claim.getTreatmentType())
                .vehicleNumber(claim.getVehicleNumber())
                .garageRepairShop(claim.getGarageRepairShop())
                .policeReportNumber(claim.getPoliceReportNumber())
                .witnessName(claim.getWitnessName())
                .witnessContact(claim.getWitnessContact())
                .pdfGenerated(Boolean.TRUE.equals(claim.getPdfGenerated()))
                .evidenceUploaded(claim.getEvidences() != null)
                .aadhaarUploaded(claim.getAadhaarCard() != null)
                .consentGiven(Boolean.TRUE.equals(claim.getConsentGiven()))
                .consentTimestamp(claim.getConsentTimestamp())
                .adminRemarks(claim.getAdminRemarks())
                .reviewedAt(claim.getReviewedAt())
                .autoApproved(Boolean.TRUE.equals(claim.getAutoApproved()))
                .timeOfCreation(claim.getTimeOfCreation())
                .lastUpdatedAt(claim.getLastUpdatedAt())
                .build();
    }
}