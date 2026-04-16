package com.smartSure.claimService.service;

import com.smartSure.claimService.client.PolicyClient;
import com.smartSure.claimService.client.UserClient;
import com.smartSure.claimService.dto.ClaimRequest;
import com.smartSure.claimService.dto.ClaimResponse;
import com.smartSure.claimService.dto.PolicyDTO;
import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.FileData;
import com.smartSure.claimService.entity.Status;
import com.smartSure.claimService.exception.ClaimDeletionNotAllowedException;
import com.smartSure.claimService.exception.ClaimNotFoundException;
import com.smartSure.claimService.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private PolicyClient policyClient;
    @Mock private UserClient userClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private FileStorageService fileStorageService;
    @Mock private PdfGenerationService pdfGenerationService;

    @InjectMocks
    private ClaimService claimService;

    private Claim mockClaim;
    private PolicyDTO mockPolicy;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(claimService, "autoApprovalThreshold", new BigDecimal("10000"));

        mockClaim = new Claim();
        mockClaim.setId(1L);
        mockClaim.setUserId(10L);
        mockClaim.setPolicyId(5L);
        mockClaim.setAmount(new BigDecimal("5000"));
        mockClaim.setStatus(Status.DRAFT);
        mockClaim.setDescription("Hospital treatment");
        mockClaim.setConsentGiven(false);

        mockPolicy = new PolicyDTO();
        mockPolicy.setId(5L);
        mockPolicy.setCustomerId(10L);
        mockPolicy.setCoverageAmount(new BigDecimal("100000"));
        // PolicyDTO has no status field — ClaimService only checks customerId and coverageAmount
    }

    // ─── Create Claim Tests ─────────────────────────────────────────────────

    @Test
    void createClaim_success() throws Exception {
        ClaimRequest request = new ClaimRequest();
        request.setPolicyId(5L);
        request.setClaimAmount(new BigDecimal("5000"));
        request.setDescription("Hospital treatment");
        request.setIncidentType(Claim.IncidentType.HEALTH);

        when(policyClient.getPolicyById(5L)).thenReturn(mockPolicy);
        when(claimRepository.existsByUserIdAndPolicyIdAndStatusNotIn(any(), any(), any()))
                .thenReturn(false);
        when(claimRepository.save(any())).thenReturn(mockClaim);
        when(pdfGenerationService.generateClaimFormPdf(any())).thenReturn("/path/to/file.pdf");

        ClaimResponse result = claimService.createClaim(request, 10L);

        assertNotNull(result);
        verify(claimRepository, atLeastOnce()).save(any());
    }

    @Test
    void createClaim_throwsWhenPolicyNotBelongsToUser() {
        ClaimRequest request = new ClaimRequest();
        request.setPolicyId(5L);
        request.setClaimAmount(new BigDecimal("5000"));

        mockPolicy.setCustomerId(99L); // Different customer
        when(policyClient.getPolicyById(5L)).thenReturn(mockPolicy);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> claimService.createClaim(request, 10L));
        assertTrue(ex.getMessage().contains("Policy does not belong to you"));
    }

    @Test
    void createClaim_throwsWhenClaimAmountExceedsCoverage() {
        ClaimRequest request = new ClaimRequest();
        request.setPolicyId(5L);
        request.setClaimAmount(new BigDecimal("200000")); // More than coverage

        when(policyClient.getPolicyById(5L)).thenReturn(mockPolicy);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> claimService.createClaim(request, 10L));
        assertTrue(ex.getMessage().contains("cannot exceed policy coverage"));
    }

    @Test
    void createClaim_throwsWhenDuplicateActiveClaim() {
        ClaimRequest request = new ClaimRequest();
        request.setPolicyId(5L);
        request.setClaimAmount(new BigDecimal("5000"));

        when(policyClient.getPolicyById(5L)).thenReturn(mockPolicy);
        when(claimRepository.existsByUserIdAndPolicyIdAndStatusNotIn(any(), any(), any()))
                .thenReturn(true); // Already has open claim

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> claimService.createClaim(request, 10L));
        assertTrue(ex.getMessage().contains("active claim"));
    }

    // ─── Give Consent Tests ─────────────────────────────────────────────────

    @Test
    void giveConsent_success() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));
        when(claimRepository.save(any())).thenReturn(mockClaim);

        ClaimResponse result = claimService.giveConsent(1L, 10L, true, "192.168.1.1");

        assertNotNull(result);
        assertTrue(mockClaim.getConsentGiven());
        assertNotNull(mockClaim.getConsentTimestamp());
    }

    @Test
    void giveConsent_throwsWhenNotInDraftStatus() {
        mockClaim.setStatus(Status.SUBMITTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThrows(IllegalStateException.class,
                () -> claimService.giveConsent(1L, 10L, true, "127.0.0.1"));
    }

    @Test
    void giveConsent_throwsWhenConsentRefused() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThrows(IllegalArgumentException.class,
                () -> claimService.giveConsent(1L, 10L, false, "127.0.0.1"));
    }

    // ─── Submit Claim Tests ─────────────────────────────────────────────────

    @Test
    void submitClaim_success() {
        mockClaim.setConsentGiven(true);
        mockClaim.setEvidences(new FileData("evidence.jpg", "image/jpeg", "/path/evidence.jpg"));

        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));
        when(policyClient.getPolicyById(5L)).thenReturn(mockPolicy);
        when(claimRepository.save(any())).thenReturn(mockClaim);

        ClaimResponse result = claimService.submitClaim(1L, 10L);

        assertNotNull(result);
        // amount=5000 is below auto-approval threshold=10000, so status becomes APPROVED
        assertEquals(Status.APPROVED, mockClaim.getStatus());
    }

    @Test
    void submitClaim_throwsWhenNoConsent() {
        mockClaim.setConsentGiven(false);
        mockClaim.setEvidences(new FileData("evidence.jpg", "image/jpeg", "/path"));

        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThrows(IllegalStateException.class,
                () -> claimService.submitClaim(1L, 10L));
    }

    @Test
    void submitClaim_throwsWhenNoEvidence() {
        mockClaim.setConsentGiven(true);
        mockClaim.setEvidences(null); // No evidence uploaded

        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThrows(Exception.class,
                () -> claimService.submitClaim(1L, 10L));
    }

    // ─── Admin Move to Status Tests ─────────────────────────────────────────

    @Test
    void moveToStatus_approveSuccess() {
        mockClaim.setStatus(Status.SUBMITTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));
        when(policyClient.getPolicyById(5L)).thenReturn(mockPolicy);
        when(claimRepository.save(any())).thenReturn(mockClaim);

        ClaimResponse result = claimService.moveToStatus(1L, Status.APPROVED, "Looks valid");

        assertNotNull(result);
        verify(claimRepository).save(any());
    }

    @Test
    void moveToStatus_rejectThrowsWhenNoRemarks() {
        mockClaim.setStatus(Status.SUBMITTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        // Rejection without remarks must be blocked (IRDAI regulation)
        assertThrows(IllegalArgumentException.class,
                () -> claimService.moveToStatus(1L, Status.REJECTED, null));
    }

    @Test
    void moveToStatus_rejectSuccess() {
        mockClaim.setStatus(Status.SUBMITTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));
        when(claimRepository.save(any())).thenReturn(mockClaim);

        ClaimResponse result = claimService.moveToStatus(1L, Status.REJECTED, "Documents incomplete");

        assertNotNull(result);
        assertEquals(Status.REJECTED, mockClaim.getStatus());
    }

    // ─── Get Claim Tests ────────────────────────────────────────────────────

    @Test
    void getClaimById_success_asAdmin() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        ClaimResponse result = claimService.getClaimById(1L, 99L, true);

        assertNotNull(result);
    }

    @Test
    void getClaimById_throwsWhenNotFound() {
        when(claimRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClaimNotFoundException.class,
                () -> claimService.getClaimById(99L, 10L, false));
    }

    @Test
    void getMyClaims_returnsListForUser() {
        when(claimRepository.findByUserId(10L)).thenReturn(List.of(mockClaim));

        List<ClaimResponse> results = claimService.getMyClaims(10L);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ─── Delete Claim Tests ─────────────────────────────────────────────────

    @Test
    void deleteClaim_success_whenDraft() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        claimService.deleteClaim(1L, 10L);

        verify(claimRepository).deleteById(1L);
        verify(fileStorageService).deleteClaimFiles(1L);
    }

    @Test
    void deleteClaim_throwsWhenNotDraft() {
        mockClaim.setStatus(Status.SUBMITTED); // Already submitted — cannot delete
        when(claimRepository.findById(1L)).thenReturn(Optional.of(mockClaim));

        assertThrows(ClaimDeletionNotAllowedException.class,
                () -> claimService.deleteClaim(1L, 10L));
        verify(claimRepository, never()).deleteById(any());
    }

    // ─── getApprovedClaimTotal Tests ────────────────────────────────────────

    @Test
    void getApprovedClaimTotal_returnsSumFromRepository() {
        when(claimRepository.sumApprovedClaimsByPolicyId(5L))
                .thenReturn(new BigDecimal("30000"));

        BigDecimal total = claimService.getApprovedClaimTotal(5L);

        assertEquals(new BigDecimal("30000"), total);
    }

    @Test
    void getApprovedClaimTotal_returnsNullWhenNoClaims() {
        when(claimRepository.sumApprovedClaimsByPolicyId(5L)).thenReturn(null);

        BigDecimal total = claimService.getApprovedClaimTotal(5L);

        assertNull(total);
    }
}
