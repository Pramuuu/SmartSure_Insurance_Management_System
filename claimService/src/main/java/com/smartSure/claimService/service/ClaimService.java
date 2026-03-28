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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyClient    policyClient;
    private final UserClient      userClient;
    private final RabbitTemplate  rabbitTemplate;

    public ClaimResponse createClaim(ClaimRequest request) {
        PolicyDTO policy = policyClient.getPolicyById(request.getPolicyId());
        Claim claim = new Claim();
        claim.setPolicyId(request.getPolicyId());
        claim.setAmount(policy.getCoverageAmount());
        return toResponse(claimRepository.save(claim));
    }

    public ClaimResponse getClaimById(Long claimId) {
        return toResponse(findOrThrow(claimId));
    }

    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ClaimResponse> getAllUnderReviewClaims() {
        return claimRepository.findByStatus(Status.UNDER_REVIEW).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PolicyDTO getPolicyForClaim(Long claimId) {
        return policyClient.getPolicyById(findOrThrow(claimId).getPolicyId());
    }

    public void deleteClaim(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getStatus() != Status.DRAFT) throw new ClaimDeletionNotAllowedException(claimId);
        claimRepository.deleteById(claimId);
    }

    // Customer submits after uploading all 3 docs — DRAFT → SUBMITTED → UNDER_REVIEW
    public ClaimResponse submitClaim(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getStatus() != Status.DRAFT)
            throw new IllegalStateException("Claim " + claimId + " cannot be submitted — not in DRAFT status.");
        if (claim.getClaimForm() == null)   throw new DocumentNotUploadedException("Claim form", claimId);
        if (claim.getAadhaarCard() == null) throw new DocumentNotUploadedException("Aadhaar card", claimId);
        if (claim.getEvidences() == null)   throw new DocumentNotUploadedException("Evidence", claimId);

        claim.setStatus(claim.getStatus().moveTo(Status.SUBMITTED));
        claim.setStatus(claim.getStatus().moveTo(Status.UNDER_REVIEW));
        return toResponse(claimRepository.save(claim));
    }

    // Admin moves claim to APPROVED or REJECTED — publishes ClaimDecisionEvent via RabbitMQ
    public ClaimResponse moveToStatus(Long claimId, Status nextStatus) {
        Claim claim = findOrThrow(claimId);
        claim.setStatus(claim.getStatus().moveTo(nextStatus));
        Claim saved = claimRepository.save(claim);

        if (nextStatus == Status.APPROVED || nextStatus == Status.REJECTED) {
            publishDecisionEvent(saved, nextStatus);
        }
        return toResponse(saved);
    }

    public ClaimResponse uploadClaimForm(Long claimId, MultipartFile file) throws IOException {
        Claim claim = findOrThrow(claimId);
        claim.setClaimForm(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    public ClaimResponse uploadAadhaarCard(Long claimId, MultipartFile file) throws IOException {
        Claim claim = findOrThrow(claimId);
        claim.setAadhaarCard(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    public ClaimResponse uploadEvidence(Long claimId, MultipartFile file) throws IOException {
        Claim claim = findOrThrow(claimId);
        claim.setEvidences(toFileData(file));
        return toResponse(claimRepository.save(claim));
    }

    public FileData downloadClaimForm(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getClaimForm() == null) throw new DocumentNotUploadedException("Claim form", claimId);
        return claim.getClaimForm();
    }

    public FileData downloadAadhaarCard(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getAadhaarCard() == null) throw new DocumentNotUploadedException("Aadhaar card", claimId);
        return claim.getAadhaarCard();
    }

    public FileData downloadEvidence(Long claimId) {
        Claim claim = findOrThrow(claimId);
        if (claim.getEvidences() == null) throw new DocumentNotUploadedException("Evidence", claimId);
        return claim.getEvidences();
    }

    // Publish claim decision event — email is handled asynchronously by a listener (or notification service)
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

    private Claim findOrThrow(Long claimId) {
        return claimRepository.findById(claimId).orElseThrow(() -> new ClaimNotFoundException(claimId));
    }

    private FileData toFileData(MultipartFile file) throws IOException {
        return new FileData(file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(), claim.getPolicyId(), claim.getAmount(), claim.getStatus(),
                claim.getTimeOfCreation(),
                claim.getClaimForm() != null, claim.getAadhaarCard() != null, claim.getEvidences() != null
        );
    }
}
