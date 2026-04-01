package com.smartSure.claimService.service;

import com.smartSure.claimService.client.PolicyClient;
import com.smartSure.claimService.client.UserClient;
import com.smartSure.claimService.dto.ClaimRequest;
import com.smartSure.claimService.dto.ClaimResponse;
import com.smartSure.claimService.dto.PolicyDTO;
import com.smartSure.claimService.dto.UserResponseDto;
import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.exception.ClaimDeletionNotAllowedException;
import com.smartSure.claimService.exception.ClaimNotFoundException;
import com.smartSure.claimService.exception.DocumentNotUploadedException;
import com.smartSure.claimService.messaging.ClaimDecisionEvent;
import com.smartSure.claimService.messaging.RabbitMQConfig;
import com.smartSure.claimService.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyClient policyClient;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ClaimResponse createClaim(ClaimRequest request, Long userId) {
        PolicyDTO policy = policyClient.getPolicyById(request.getPolicyId());

        if (!policy.getCustomerId().equals(userId)) {
            throw new RuntimeException("Policy does not belong to you");
        }

        Claim claim = new Claim();
        claim.setPolicyId(request.getPolicyId());
        claim.setUserId(userId);
        claim.setAmount(policy.getCoverageAmount());
        claim.setDescription(request.getDescription());

        if (request.getIncidentDate() != null && !request.getIncidentDate().isBlank()) {
            claim.setIncidentDate(LocalDate.parse(request.getIncidentDate()));
        }

        return toResponse(claimRepository.save(claim));
    }

    public ClaimResponse getClaimById(Long claimId, Long userId, boolean isAdmin) {
        return toResponse(findOrThrow(claimId, userId, isAdmin));
    }

    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ClaimResponse> getMyClaims(Long userId) {
        return claimRepository.findByUserId(userId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ClaimResponse> getAllUnderReviewClaims() {
        return claimRepository.findByStatus(Status.UNDER_REVIEW).stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PolicyDTO getPolicyForClaim(Long claimId, Long userId, boolean isAdmin) {
        return policyClient.getPolicyById(findOrThrow(claimId, userId, isAdmin).getPolicyId());
    }

    @Transactional
    public void deleteClaim(Long claimId, Long userId) {
        Claim claim = findOrThrow(claimId, userId, false);
        if (claim.getStatus() != Status.DRAFT)
            throw new ClaimDeletionNotAllowedException(claimId);
        claimRepository.deleteById(claimId);
    }

    // Customer submits after uploading all 3 docs — DRAFT → SUBMITTED →
    // UNDER_REVIEW
    @Transactional
    public ClaimResponse submitClaim(Long claimId, Long userId) {
        Claim claim = findOrThrow(claimId, userId, false);
        if (claim.getStatus() != Status.DRAFT)
            throw new IllegalStateException("Claim " + claimId + " cannot be submitted — not in DRAFT status.");
        if (claim.getClaimForm() == null)
            throw new DocumentNotUploadedException("Claim form", claimId);
        if (claim.getAadhaarCard() == null)
            throw new DocumentNotUploadedException("Aadhaar card", claimId);
        if (claim.getEvidences() == null)
            throw new DocumentNotUploadedException("Evidence", claimId);

        claim.setStatus(claim.getStatus().moveTo(Status.SUBMITTED));
        claim.setStatus(claim.getStatus().moveTo(Status.UNDER_REVIEW));
        return toResponse(claimRepository.save(claim));
    }

    // Admin moves claim to APPROVED or REJECTED — publishes ClaimDecisionEvent via
    // RabbitMQ
    @Transactional
    public ClaimResponse moveToStatus(Long claimId, Status nextStatus) {
        Claim claim = findOrThrow(claimId, null, true);
        claim.setStatus(claim.getStatus().moveTo(nextStatus));
        Claim saved = claimRepository.save(claim);

        if (nextStatus == Status.APPROVED || nextStatus == Status.REJECTED) {
            publishDecisionEvent(saved, nextStatus);
        }
        return toResponse(saved);
    }

    @Transactional
    public ClaimResponse uploadClaimForm(Long claimId, MultipartFile file, Long userId) throws IOException {
        Claim claim = findOrThrow(claimId, userId, false);
        claim.setClaimForm(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    @Transactional
    public ClaimResponse uploadAadhaarCard(Long claimId, MultipartFile file, Long userId) throws IOException {
        Claim claim = findOrThrow(claimId, userId, false);
        claim.setAadhaarCard(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    @Transactional
    public ClaimResponse uploadEvidence(Long claimId, MultipartFile file, Long userId) throws IOException {
        Claim claim = findOrThrow(claimId, userId, false);
        claim.setEvidences(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    public FileData downloadClaimForm(Long claimId, Long userId, boolean isAdmin) {
        Claim claim = findOrThrow(claimId, userId, isAdmin);
        if (claim.getClaimForm() == null)
            throw new DocumentNotUploadedException("Claim form", claimId);
        return claim.getClaimForm();
    }

    public FileData downloadAadhaarCard(Long claimId, Long userId, boolean isAdmin) {
        Claim claim = findOrThrow(claimId, userId, isAdmin);
        if (claim.getAadhaarCard() == null)
            throw new DocumentNotUploadedException("Aadhaar card", claimId);
        return claim.getAadhaarCard();
    }

    public FileData downloadEvidence(Long claimId, Long userId, boolean isAdmin) {
        Claim claim = findOrThrow(claimId, userId, isAdmin);
        if (claim.getEvidences() == null)
            throw new DocumentNotUploadedException("Evidence", claimId);
        return claim.getEvidences();
    }

    private void publishDecisionEvent(Claim claim, Status decision) {
        try {
            PolicyDTO policy = policyClient.getPolicyById(claim.getPolicyId());
            UserResponseDto user = userClient.getUserById(policy.getCustomerId());

            ClaimDecisionEvent event = ClaimDecisionEvent.builder()
                    .claimId(claim.getId())
                    .policyId(claim.getPolicyId())
                    .decision(decision.name())
                    .amount(claim.getAmount())
                    .customerEmail(user.getEmail())
                    .customerName(user.getFirstName())
                    .decidedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.CLAIM_DECISION_KEY, event);
            log.info("ClaimDecisionEvent published — claimId={}, decision={}", claim.getId(), decision);
        } catch (Exception e) {
            log.error("Failed to publish ClaimDecisionEvent for claim {}: {}", claim.getId(), e.getMessage());
        }
    }

    private Claim findOrThrow(Long claimId, Long userId, boolean isAdmin) {
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new ClaimNotFoundException(claimId));
        if (!isAdmin && userId != null && !claim.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only access your own claims");
        }
        return claim;
    }

    private FileData toFileData(MultipartFile file) throws IOException {
        return new FileData(file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(), claim.getPolicyId(), claim.getUserId(), claim.getAmount(), claim.getStatus(),
                claim.getDescription(), claim.getIncidentDate(), claim.getTimeOfCreation(),
                claim.getClaimForm() != null, claim.getAadhaarCard() != null, claim.getEvidences() != null);
    }
}
