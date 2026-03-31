package com.smartSure.PolicyService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartSure.PolicyService.dto.policy.*;
import com.smartSure.PolicyService.dto.premium.PremiumPaymentRequest;
import com.smartSure.PolicyService.dto.premium.PremiumResponse;
import com.smartSure.PolicyService.entity.Policy;
import com.smartSure.PolicyService.entity.Premium;
import com.smartSure.PolicyService.exception.*;
import com.smartSure.PolicyService.security.SecurityUtils;
import com.smartSure.PolicyService.service.PolicyService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice tests using @WebMvcTest.
 *
 * @WebMvcTest loads ONLY the web layer — no database, no service beans.
 * PolicyService is replaced by a @MockBean.
 * Security is applied via @WithMockUser for role simulation.
 *
 * NOTE: Because SecurityConfig in this project uses InternalRequestFilter
 * and HeaderAuthenticationFilter (not the default form login), you need to
 * add @WithMockUser to supply Spring Security's test authentication context.
 * In a full integration test you would populate the SecurityContextHolder directly.
 */
@WebMvcTest(PolicyController.class)
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyController Web Layer Tests")
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PolicyService policyService;

    private ObjectMapper objectMapper;

    // Shared response fixtures
    private PolicyResponse samplePolicyResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        samplePolicyResponse = PolicyResponse.builder()
                .id(1L)
                .policyNumber("POL-20250101-ABCDE")
                .customerId(42L)
                .status("ACTIVE")
                .coverageAmount(new BigDecimal("500000.00"))
                .premiumAmount(new BigDecimal("2625.00"))
                .paymentFrequency("MONTHLY")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // POST /api/policies/purchase
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/policies/purchase")
    class PurchasePolicyEndpoint {

        private PolicyPurchaseRequest buildValidRequest() {
            return PolicyPurchaseRequest.builder()
                    .policyTypeId(10L)
                    .coverageAmount(new BigDecimal("500000.00"))
                    .paymentFrequency(Policy.PaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .customerAge(30)
                    .build();
        }

        @Test
        @DisplayName("should return 201 CREATED for valid purchase request")
        @WithMockUser(roles = "CUSTOMER")
        void purchasePolicy_validRequest_returns201() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);

                when(policyService.purchasePolicy(eq(42L), any(PolicyPurchaseRequest.class)))
                        .thenReturn(samplePolicyResponse);

                mockMvc.perform(post("/api/policies/purchase")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildValidRequest())))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").value(1L))
                        .andExpect(jsonPath("$.policyNumber").value("POL-20250101-ABCDE"))
                        .andExpect(jsonPath("$.status").value("ACTIVE"));
            }
        }

        @Test
        @DisplayName("should return 409 when duplicate policy exception is thrown")
        @WithMockUser(roles = "CUSTOMER")
        void purchasePolicy_duplicate_returns409() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);

                when(policyService.purchasePolicy(any(), any()))
                        .thenThrow(new DuplicatePolicyException());

                mockMvc.perform(post("/api/policies/purchase")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(buildValidRequest())))
                        .andExpect(status().isConflict());
            }
        }

        @Test
        @DisplayName("should return 400 when required fields are missing")
        @WithMockUser(roles = "CUSTOMER")
        void purchasePolicy_missingFields_returns400() throws Exception {
            // Empty body — all @NotNull fields will fail validation
            mockMvc.perform(post("/api/policies/purchase")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when ADMIN role tries to purchase")
        @WithMockUser(roles = "ADMIN")
        void purchasePolicy_adminRole_returns403() throws Exception {
            mockMvc.perform(post("/api/policies/purchase")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GET /api/policies/my
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/policies/my")
    class GetMyPoliciesEndpoint {

        @Test
        @DisplayName("should return 200 with paginated policies")
        @WithMockUser(roles = "CUSTOMER")
        void getMyPolicies_returns200() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);

                PolicyPageResponse pageResponse = PolicyPageResponse.builder()
                        .content(List.of(samplePolicyResponse))
                        .pageNumber(0)
                        .pageSize(10)
                        .totalElements(1L)
                        .totalPages(1)
                        .last(true)
                        .build();

                when(policyService.getCustomerPolicies(eq(42L), any(Pageable.class)))
                        .thenReturn(pageResponse);

                mockMvc.perform(get("/api/policies/my")
                                .param("page", "0")
                                .param("size", "10"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content").isArray())
                        .andExpect(jsonPath("$.content[0].id").value(1L))
                        .andExpect(jsonPath("$.totalElements").value(1L))
                        .andExpect(jsonPath("$.pageNumber").value(0));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GET /api/policies/{policyId}
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/policies/{policyId}")
    class GetPolicyByIdEndpoint {

        @Test
        @DisplayName("should return 200 with policy details")
        @WithMockUser(roles = "CUSTOMER")
        void getPolicyById_found_returns200() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);
                securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("ROLE_CUSTOMER");

                when(policyService.getPolicyById(1L, 42L, false))
                        .thenReturn(samplePolicyResponse);

                mockMvc.perform(get("/api/policies/1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(1L))
                        .andExpect(jsonPath("$.policyNumber").value("POL-20250101-ABCDE"));
            }
        }

        @Test
        @DisplayName("should return 404 when policy not found")
        @WithMockUser(roles = "CUSTOMER")
        void getPolicyById_notFound_returns404() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);
                securityUtils.when(SecurityUtils::getCurrentRole).thenReturn("ROLE_CUSTOMER");

                when(policyService.getPolicyById(99L, 42L, false))
                        .thenThrow(new PolicyNotFoundException(99L));

                mockMvc.perform(get("/api/policies/99"))
                        .andExpect(status().isNotFound());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PUT /api/policies/{policyId}/cancel
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/policies/{policyId}/cancel")
    class CancelPolicyEndpoint {

        @Test
        @DisplayName("should return 200 when policy is cancelled successfully")
        @WithMockUser(roles = "CUSTOMER")
        void cancelPolicy_success_returns200() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);

                PolicyResponse cancelledResponse = PolicyResponse.builder()
                        .id(1L).status("CANCELLED").build();

                when(policyService.cancelPolicy(1L, 42L, "No longer needed"))
                        .thenReturn(cancelledResponse);

                mockMvc.perform(put("/api/policies/1/cancel")
                                .with(csrf())
                                .param("reason", "No longer needed"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("CANCELLED"));
            }
        }

        @Test
        @DisplayName("should return 409 when policy is already cancelled")
        @WithMockUser(roles = "CUSTOMER")
        void cancelPolicy_alreadyCancelled_returns409() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);

                when(policyService.cancelPolicy(any(), any(), any()))
                        .thenThrow(new IllegalStateException("Policy is already cancelled"));

                mockMvc.perform(put("/api/policies/1/cancel")
                                .with(csrf()))
                        .andExpect(status().isConflict());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // POST /api/policies/premiums/pay
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/policies/premiums/pay")
    class PayPremiumEndpoint {

        @Test
        @DisplayName("should return 200 when premium is paid successfully")
        @WithMockUser(roles = "CUSTOMER")
        void payPremium_success_returns200() throws Exception {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(42L);

                PremiumPaymentRequest request = PremiumPaymentRequest.builder()
                        .policyId(1L)
                        .premiumId(1L)
                        .paymentMethod(Premium.PaymentMethod.UPI)
                        .paymentReference("TXN-123456")
                        .build();

                PremiumResponse premiumResponse = PremiumResponse.builder()
                        .id(1L)
                        .amount(new BigDecimal("2625.00"))
                        .status("PAID")
                        .paymentMethod("UPI")
                        .paymentReference("TXN-123456")
                        .paidDate(LocalDate.now())
                        .build();

                when(policyService.payPremium(eq(42L), any(PremiumPaymentRequest.class)))
                        .thenReturn(premiumResponse);

                mockMvc.perform(post("/api/policies/premiums/pay")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("PAID"))
                        .andExpect(jsonPath("$.paymentReference").value("TXN-123456"));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // GET /api/policies/admin/summary
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/policies/admin/summary")
    class AdminSummaryEndpoint {

        @Test
        @DisplayName("should return 200 with summary for ADMIN role")
        @WithMockUser(roles = "ADMIN")
        void getSummary_adminRole_returns200() throws Exception {
            PolicySummaryResponse summary = PolicySummaryResponse.builder()
                    .totalPolicies(100L)
                    .activePolicies(70L)
                    .expiredPolicies(20L)
                    .cancelledPolicies(10L)
                    .totalPremiumCollected(new BigDecimal("500000.00"))
                    .totalCoverageProvided(new BigDecimal("50000000.00"))
                    .build();

            when(policyService.getPolicySummary()).thenReturn(summary);

            mockMvc.perform(get("/api/policies/admin/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPolicies").value(100))
                    .andExpect(jsonPath("$.activePolicies").value(70));
        }

        @Test
        @DisplayName("should return 403 when CUSTOMER role tries to access admin summary")
        @WithMockUser(roles = "CUSTOMER")
        void getSummary_customerRole_returns403() throws Exception {
            mockMvc.perform(get("/api/policies/admin/summary"))
                    .andExpect(status().isForbidden());
        }
    }
}