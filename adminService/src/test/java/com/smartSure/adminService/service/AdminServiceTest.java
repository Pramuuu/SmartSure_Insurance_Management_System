package com.smartSure.adminService.service;

import com.smartSure.adminService.dto.*;
import com.smartSure.adminService.feign.ClaimFeignClient;
import com.smartSure.adminService.feign.PaymentFeignClient;
import com.smartSure.adminService.feign.PolicyFeignClient;
import com.smartSure.adminService.feign.UserFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private ClaimFeignClient claimFeignClient;
    @Mock private PolicyFeignClient policyFeignClient;
    @Mock private UserFeignClient userFeignClient;
    @Mock private PaymentFeignClient paymentFeignClient;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AdminService adminService;

    private ClaimDTO mockClaim;
    private PolicyDTO mockPolicy;
    private UserDTO mockUser;
    private PaymentDTO mockPayment;

    @BeforeEach
    void setUp() {
        mockClaim = new ClaimDTO();
        mockClaim.setId(1L);
        mockClaim.setStatus("SUBMITTED");
        mockClaim.setAmount(new BigDecimal("5000"));
        mockClaim.setAutoApproved(false);

        mockPolicy = new PolicyDTO();
        mockPolicy.setId(1L);
        mockPolicy.setStatus("ACTIVE");
        mockPolicy.setCustomerId(10L);
        mockPolicy.setCoverageAmount(new BigDecimal("100000"));
        mockPolicy.setPremiumAmount(new BigDecimal("500"));

        mockUser = new UserDTO();
        mockUser.setId(10L);
        mockUser.setEmail("customer@example.com");

        mockPayment = new PaymentDTO();
        mockPayment.setId(1L);
        mockPayment.setAmount(new BigDecimal("500"));
        mockPayment.setStatus("SUCCESS");
    }

    // ─── Dashboard Tests ────────────────────────────────────────────────────

    @Test
    void getDashboardMetrics_success() {
        when(userFeignClient.getAllUsers()).thenReturn(List.of(mockUser));
        when(policyFeignClient.getAllPolicies()).thenReturn(List.of(mockPolicy));
        when(claimFeignClient.getAllClaims()).thenReturn(List.of(mockClaim));
        when(auditLogService.getRecentLogs(5)).thenReturn(Collections.emptyList());

        DashboardMetricsResponse result = adminService.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(1L, result.getTotalUsers());
        assertEquals(1L, result.getTotalPolicies());
        assertEquals(1L, result.getTotalClaims());
        // Our mock claim is SUBMITTED so it counts as pending
        assertEquals(1L, result.getPendingClaims());
    }

    @Test
    void getDashboardMetrics_returnsZeroWhenServiceDown() {
        // All downstream services fail — dashboard should still return 0s, not throw
        when(userFeignClient.getAllUsers()).thenThrow(new RuntimeException("UserService down"));
        when(policyFeignClient.getAllPolicies()).thenThrow(new RuntimeException("PolicyService down"));
        when(claimFeignClient.getAllClaims()).thenThrow(new RuntimeException("ClaimService down"));
        when(auditLogService.getRecentLogs(5)).thenReturn(Collections.emptyList());

        DashboardMetricsResponse result = adminService.getDashboardMetrics();

        assertNotNull(result);
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getTotalPolicies());
        assertEquals(0L, result.getTotalClaims());
    }

    @Test
    void getPolicyStats_countsCorrectly() {
        PolicyDTO expiredPolicy = new PolicyDTO();
        expiredPolicy.setStatus("EXPIRED");
        expiredPolicy.setCoverageAmount(new BigDecimal("50000"));
        expiredPolicy.setPremiumAmount(new BigDecimal("300"));

        when(policyFeignClient.getAllPolicies()).thenReturn(List.of(mockPolicy, expiredPolicy));

        PolicyStatsResponse result = adminService.getPolicyStats();

        assertNotNull(result);
        assertEquals(2, result.getTotalPolicies());
        assertEquals(1L, result.getActivePolicies());
        assertEquals(1L, result.getExpiredPolicies());
    }

    @Test
    void getClaimStats_countsApprovedAmountCorrectly() {
        ClaimDTO approvedClaim = new ClaimDTO();
        approvedClaim.setStatus("APPROVED");
        approvedClaim.setAmount(new BigDecimal("8000"));
        approvedClaim.setAutoApproved(false);

        when(claimFeignClient.getAllClaims()).thenReturn(List.of(mockClaim, approvedClaim));

        ClaimStatsResponse result = adminService.getClaimStats();

        assertNotNull(result);
        assertEquals(2, result.getTotalClaims());
        assertEquals(1L, result.getApprovedClaims());
        assertEquals(new BigDecimal("8000"), result.getTotalApprovedAmount());
    }

    // ─── Claim Management Tests ─────────────────────────────────────────────

    @Test
    void approveClaim_success() {
        ClaimDTO approved = new ClaimDTO();
        approved.setId(1L);
        approved.setStatus("APPROVED");

        when(claimFeignClient.updateClaimStatus(eq(1L), any())).thenReturn(approved);

        ClaimDTO result = adminService.approveClaim(99L, 1L, "Documents verified");

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        verify(auditLogService).log(99L, "APPROVE_CLAIM", "Claim", 1L, "Documents verified");
    }

    @Test
    void rejectClaim_success() {
        ClaimDTO rejected = new ClaimDTO();
        rejected.setId(1L);
        rejected.setStatus("REJECTED");

        when(claimFeignClient.updateClaimStatus(eq(1L), any())).thenReturn(rejected);

        ClaimDTO result = adminService.rejectClaim(99L, 1L, "Insufficient documents");

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        verify(auditLogService).log(99L, "REJECT_CLAIM", "Claim", 1L, "Insufficient documents");
    }

    @Test
    void getAllClaims_returnsList() {
        when(claimFeignClient.getAllClaims()).thenReturn(List.of(mockClaim));

        List<ClaimDTO> result = adminService.getAllClaims();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getPendingClaims_returnsOnlySubmittedAndUnderReview() {
        ClaimDTO approvedClaim = new ClaimDTO();
        approvedClaim.setStatus("APPROVED");

        ClaimDTO underReviewClaim = new ClaimDTO();
        underReviewClaim.setStatus("UNDER_REVIEW");

        // mockClaim is SUBMITTED
        when(claimFeignClient.getAllClaims())
                .thenReturn(List.of(mockClaim, approvedClaim, underReviewClaim));

        List<ClaimDTO> result = adminService.getPendingClaims();

        assertEquals(2, result.size()); // Only SUBMITTED + UNDER_REVIEW
    }

    @Test
    void markUnderReview_success_whenSubmitted() {
        ClaimDTO underReview = new ClaimDTO();
        underReview.setId(1L);
        underReview.setStatus("UNDER_REVIEW");

        when(claimFeignClient.getClaimById(1L)).thenReturn(mockClaim); // SUBMITTED
        when(claimFeignClient.updateClaimStatus(eq(1L), any())).thenReturn(underReview);

        ClaimDTO result = adminService.markUnderReview(99L, 1L);

        assertEquals("UNDER_REVIEW", result.getStatus());
        verify(claimFeignClient).updateClaimStatus(eq(1L), any());
    }

    @Test
    void markUnderReview_skipUpdateWhenAlreadyUnderReview() {
        mockClaim.setStatus("UNDER_REVIEW");
        when(claimFeignClient.getClaimById(1L)).thenReturn(mockClaim);

        ClaimDTO result = adminService.markUnderReview(99L, 1L);

        // Should return current claim without calling updateClaimStatus
        verify(claimFeignClient, never()).updateClaimStatus(any(), any());
        assertEquals("UNDER_REVIEW", result.getStatus());
    }

    // ─── Policy Management Tests ────────────────────────────────────────────

    @Test
    void getAllPolicies_returnsList() {
        when(policyFeignClient.getAllPolicies()).thenReturn(List.of(mockPolicy));

        List<PolicyDTO> result = adminService.getAllPolicies();

        assertEquals(1, result.size());
    }

    @Test
    void cancelPolicy_success() {
        PolicyDTO cancelled = new PolicyDTO();
        cancelled.setId(1L);
        cancelled.setStatus("CANCELLED");

        when(policyFeignClient.updatePolicyStatus(eq(1L), any())).thenReturn(cancelled);

        PolicyDTO result = adminService.cancelPolicy(99L, 1L, "Fraud detected");

        assertEquals("CANCELLED", result.getStatus());
        verify(auditLogService).log(99L, "CANCEL_POLICY", "Policy", 1L, "Fraud detected");
    }

    @Test
    void getPoliciesByUser_filtersCorrectly() {
        PolicyDTO otherUserPolicy = new PolicyDTO();
        otherUserPolicy.setCustomerId(99L);
        otherUserPolicy.setStatus("ACTIVE");

        when(policyFeignClient.getAllPolicies()).thenReturn(List.of(mockPolicy, otherUserPolicy));

        List<PolicyDTO> result = adminService.getPoliciesByUser(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getCustomerId());
    }

    // ─── User Management Tests ──────────────────────────────────────────────

    @Test
    void getAllUsers_returnsList() {
        when(userFeignClient.getAllUsers()).thenReturn(List.of(mockUser));

        List<UserDTO> result = adminService.getAllUsers();

        assertEquals(1, result.size());
    }

    @Test
    void deactivateUser_success() {
        when(userFeignClient.deactivateUser(10L)).thenReturn(mockUser);

        UserDTO result = adminService.deactivateUser(99L, 10L);

        assertNotNull(result);
        verify(auditLogService).log(99L, "DEACTIVATE_USER", "User", 10L,
                "User account deactivated by admin");
    }

    // ─── Payment Management Tests ───────────────────────────────────────────

    @Test
    void getAllPayments_returnsList() {
        when(paymentFeignClient.getAllPayments()).thenReturn(List.of(mockPayment));

        List<PaymentDTO> result = adminService.getAllPayments();

        assertEquals(1, result.size());
    }

    @Test
    void getPaymentById_success() {
        when(paymentFeignClient.getPaymentById(1L)).thenReturn(mockPayment);

        PaymentDTO result = adminService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getPaymentsByCustomer_returnsList() {
        when(paymentFeignClient.getPaymentsByCustomer(10L)).thenReturn(List.of(mockPayment));

        List<PaymentDTO> result = adminService.getPaymentsByCustomer(10L);

        assertEquals(1, result.size());
    }
}
