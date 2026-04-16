package com.smartSure.PolicyService.service;

import com.smartSure.PolicyService.client.AuthServiceClient;
import com.smartSure.PolicyService.client.InternalClaimClient;
import com.smartSure.PolicyService.service.impl.PolicyServiceImpl;
import com.smartSure.PolicyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.PolicyService.dto.policy.*;
import com.smartSure.PolicyService.dto.premium.PremiumPaymentRequest;
import com.smartSure.PolicyService.dto.premium.PremiumResponse;
import com.smartSure.PolicyService.entity.*;
import com.smartSure.PolicyService.exception.*;
import com.smartSure.PolicyService.mapper.PolicyMapper;
import com.smartSure.PolicyService.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PolicyService.
 *
 * Strategy:
 *  - Every collaborator (repository, mapper, calculator, publisher, feign) is @Mock.
 *  - PolicyService gets all mocks injected via @InjectMocks.
 *  - No Spring context, no database, no RabbitMQ — pure in-memory execution.
 *  - Each test follows the AAA pattern: Arrange → Act → Assert.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService Unit Tests")
class PolicyServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────────
    @Mock private PolicyRepository       policyRepository;
    @Mock private PolicyTypeRepository   policyTypeRepository;
    @Mock private PremiumRepository      premiumRepository;
    @Mock private AuditLogRepository     auditLogRepository;
    @Mock private PremiumCalculator      premiumCalculator;
    @Mock private PolicyMapper           policyMapper;
    @Mock private NotificationPublisher  notificationPublisher;
    @Mock private AuthServiceClient      authServiceClient;
    @Mock private InternalClaimClient    internalClaimClient;

    // ── Subject under test ─────────────────────────────────────────────────────
    @InjectMocks
    private PolicyServiceImpl policyService;

    // ── Shared test fixtures ───────────────────────────────────────────────────
    private PolicyType     activePolicyType;
    private Policy         savedPolicy;
    private PolicyResponse policyResponse;
    private final Long     CUSTOMER_ID  = 42L;
    private final Long     POLICY_ID    = 1L;
    private final Long     POLICY_TYPE_ID = 10L;

    @BeforeEach
    void setUp() {
        // Build a reusable active PolicyType
        activePolicyType = PolicyType.builder()
                .id(POLICY_TYPE_ID)
                .name("Health Insurance")
                .category(PolicyType.InsuranceCategory.HEALTH)
                .basePremium(new BigDecimal("500.00"))
                .maxCoverageAmount(new BigDecimal("1000000.00"))
                .deductibleAmount(new BigDecimal("5000.00"))
                .termMonths(12)
                .status(PolicyType.PolicyTypeStatus.ACTIVE)
                .build();

        // Build a reusable saved Policy entity
        savedPolicy = Policy.builder()
                .id(POLICY_ID)
                .policyNumber("POL-20250101-ABCDE")
                .customerId(CUSTOMER_ID)
                .policyType(activePolicyType)
                .coverageAmount(new BigDecimal("500000.00"))
                .premiumAmount(new BigDecimal("2625.00"))
                .paymentFrequency(Policy.PaymentFrequency.MONTHLY)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        // Build a minimal PolicyResponse DTO
        policyResponse = PolicyResponse.builder()
                .id(POLICY_ID)
                .policyNumber("POL-20250101-ABCDE")
                .customerId(CUSTOMER_ID)
                .status("ACTIVE")
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // purchasePolicy — happy path + all exception branches
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("purchasePolicy()")
    class PurchasePolicyTests {

        private PolicyPurchaseRequest buildValidRequest() {
            return PolicyPurchaseRequest.builder()
                    .policyTypeId(POLICY_TYPE_ID)
                    .coverageAmount(new BigDecimal("500000.00"))
                    .paymentFrequency(Policy.PaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now().plusDays(1)) // Future date to avoid validation error
                    .customerAge(30)
                    .build();
        }

        @Test
        @DisplayName("should purchase policy successfully and return PolicyResponse")
        void purchasePolicy_happyPath_returnsPolicyResponse() {
            // Arrange
            PolicyPurchaseRequest request = buildValidRequest();

            PremiumCalculationResponse calcResponse = PremiumCalculationResponse.builder()
                    .calculatedPremium(new BigDecimal("2625.00"))
                    .build();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType));
            when(policyRepository.findFirstByCustomerIdAndPolicyType_IdAndStatusIn(
                    eq(CUSTOMER_ID), eq(POLICY_TYPE_ID), anyList()))
                    .thenReturn(Optional.empty());
            when(premiumCalculator.calculatePremium(
                    eq(activePolicyType), any(), any(), anyInt()))
                    .thenReturn(calcResponse);
            when(policyMapper.toEntity(request))
                    .thenReturn(savedPolicy);
            when(policyRepository.save(any())).thenReturn(savedPolicy);
            when(premiumRepository.findByPolicyId(POLICY_ID))
                    .thenReturn(List.of());
            when(policyMapper.toResponseWithPremiums(eq(savedPolicy), anyList()))
                    .thenReturn(policyResponse);
            when(authServiceClient.getCustomerProfile(CUSTOMER_ID))
                    .thenReturn(new com.smartSure.PolicyService.dto.client.CustomerProfileResponse(CUSTOMER_ID, "Test", "customer@email.com", "1234567890", 30));
            when(premiumCalculator.monthsBetweenInstallments(any()))
                    .thenReturn(1);
            when(premiumCalculator.installmentCount(anyInt(), any()))
                    .thenReturn(12);

            // Act
            PolicyResponse result = policyService.purchasePolicy(CUSTOMER_ID, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(POLICY_ID);

            // Verify critical interactions
            verify(policyRepository, times(1)).save(any(Policy.class));
            verify(premiumRepository, times(1)).saveAll(anyList());
            verify(auditLogRepository, times(1)).save(any(AuditLog.class));
            verify(notificationPublisher, times(1)).publishPolicyPurchased(any());
        }

        @Test
        @DisplayName("should throw PolicyTypeNotFoundException when policy type does not exist")
        void purchasePolicy_policyTypeNotFound_throwsException() {
            // Arrange
            PolicyPurchaseRequest request = buildValidRequest();
            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> policyService.purchasePolicy(CUSTOMER_ID, request))
                    .isInstanceOf(PolicyTypeNotFoundException.class)
                    .hasMessageContaining("10");

            // Nothing should be saved
            verify(policyRepository, never()).save(any());
            verify(notificationPublisher, never()).publishPolicyPurchased(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when startDate is in the past")
        void purchasePolicy_pastStartDate_throwsException() {
            // Arrange
            PolicyPurchaseRequest request = PolicyPurchaseRequest.builder()
                    .policyTypeId(POLICY_TYPE_ID)
                    .coverageAmount(new BigDecimal("500000.00"))
                    .paymentFrequency(Policy.PaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now().minusDays(1)) // Past date
                    .customerAge(30)
                    .build();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType));

            // Act & Assert
            assertThatThrownBy(() -> policyService.purchasePolicy(CUSTOMER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date cannot be in the past");

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when startDate is null")
        void purchasePolicy_nullStartDate_throwsException() {
            // Arrange
            PolicyPurchaseRequest request = PolicyPurchaseRequest.builder()
                    .policyTypeId(POLICY_TYPE_ID)
                    .coverageAmount(new BigDecimal("500000.00"))
                    .paymentFrequency(Policy.PaymentFrequency.MONTHLY)
                    .startDate(null) // Null date
                    .customerAge(30)
                    .build();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType));

            // Act & Assert
            assertThatThrownBy(() -> policyService.purchasePolicy(CUSTOMER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date is required");

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InactivePolicyTypeException when policy type is DISCONTINUED")
        void purchasePolicy_inactivePolicyType_throwsException() {
            // Arrange
            activePolicyType.setStatus(PolicyType.PolicyTypeStatus.DISCONTINUED);
            PolicyPurchaseRequest request = buildValidRequest();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType));

            // Act & Assert
            assertThatThrownBy(() -> policyService.purchasePolicy(CUSTOMER_ID, request))
                    .isInstanceOf(InactivePolicyTypeException.class)
                    .hasMessageContaining("Health Insurance");

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicatePolicyException when active policy already exists")
        void purchasePolicy_duplicatePolicy_throwsException() {
            // Arrange
            PolicyPurchaseRequest request = buildValidRequest();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType));
            when(policyRepository.findFirstByCustomerIdAndPolicyType_IdAndStatusIn(
                    eq(CUSTOMER_ID), eq(POLICY_TYPE_ID), anyList()))
                    .thenReturn(Optional.of(savedPolicy));
            when(internalClaimClient.getTotalApprovedClaimsAmount(POLICY_ID))
                    .thenReturn(BigDecimal.ZERO); // Coverage not exhausted

            // Act & Assert
            assertThatThrownBy(() -> policyService.purchasePolicy(CUSTOMER_ID, request))
                    .isInstanceOf(DuplicatePolicyException.class);

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw CoverageExceedsLimitException when coverage exceeds max")
        void purchasePolicy_coverageExceedsLimit_throwsException() {
            // Arrange
            // activePolicyType.maxCoverageAmount = 1,000,000 — request asks for 2,000,000
            PolicyPurchaseRequest request = PolicyPurchaseRequest.builder()
                    .policyTypeId(POLICY_TYPE_ID)
                    .coverageAmount(new BigDecimal("2000000.00"))
                    .paymentFrequency(Policy.PaymentFrequency.MONTHLY)
                    .startDate(LocalDate.now())
                    .customerAge(30)
                    .build();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType));
            when(policyRepository.findFirstByCustomerIdAndPolicyType_IdAndStatusIn(
                    any(), any(), anyList()))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> policyService.purchasePolicy(CUSTOMER_ID, request))
                    .isInstanceOf(CoverageExceedsLimitException.class);

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set status CREATED for all new policies")
        void purchasePolicy_allPolicies_statusIsCreated() {
            // Arrange
            PolicyPurchaseRequest request = buildValidRequest();

            PremiumCalculationResponse calcResponse = PremiumCalculationResponse.builder()
                    .calculatedPremium(new BigDecimal("2625.00"))
                    .build();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType));
            when(policyRepository.findFirstByCustomerIdAndPolicyType_IdAndStatusIn(
                    any(), any(), anyList())).thenReturn(Optional.empty());
            when(premiumCalculator.calculatePremium(any(), any(), any(), anyInt()))
                    .thenReturn(calcResponse);
            when(policyMapper.toEntity(request)).thenReturn(savedPolicy);

            ArgumentCaptor<Policy> policyCaptor = ArgumentCaptor.forClass(Policy.class);
            when(policyRepository.save(policyCaptor.capture())).thenReturn(savedPolicy);
            when(premiumRepository.findByPolicyId(any())).thenReturn(List.of());
            when(policyMapper.toResponseWithPremiums(any(), anyList())).thenReturn(policyResponse);
            when(premiumCalculator.monthsBetweenInstallments(any())).thenReturn(1);
            when(premiumCalculator.installmentCount(anyInt(), any())).thenReturn(12);
            when(authServiceClient.getCustomerProfile(CUSTOMER_ID))
                    .thenReturn(new com.smartSure.PolicyService.dto.client.CustomerProfileResponse(CUSTOMER_ID, "Test", "customer@email.com", "1234567890", 30));

            // Act
            policyService.purchasePolicy(CUSTOMER_ID, request);

            // Assert — all new policies start with CREATED status
            assertThat(policyCaptor.getValue().getStatus())
                    .isEqualTo(Policy.PolicyStatus.CREATED);
        }

@Test
        @DisplayName("should generate correct number of premium installments for MONTHLY frequency")
        void purchasePolicy_monthlyFrequency_generates12Installments() {
            // Arrange
            PolicyPurchaseRequest request = buildValidRequest();

            when(policyTypeRepository.findById(POLICY_TYPE_ID))
                    .thenReturn(Optional.of(activePolicyType)); // termMonths = 12
            when(policyRepository.findFirstByCustomerIdAndPolicyType_IdAndStatusIn(
                    any(), any(), anyList())).thenReturn(Optional.empty());
            when(premiumCalculator.calculatePremium(any(), any(), any(), anyInt()))
                    .thenReturn(PremiumCalculationResponse.builder()
                            .calculatedPremium(new BigDecimal("2625.00")).build());
            when(policyMapper.toEntity(request)).thenReturn(savedPolicy);
            when(policyRepository.save(any())).thenReturn(savedPolicy);
            when(premiumRepository.findByPolicyId(any())).thenReturn(List.of());
            when(policyMapper.toResponseWithPremiums(any(), anyList())).thenReturn(policyResponse);
            when(premiumCalculator.monthsBetweenInstallments(Policy.PaymentFrequency.MONTHLY))
                    .thenReturn(1);
            when(premiumCalculator.installmentCount(12, Policy.PaymentFrequency.MONTHLY))
                    .thenReturn(12);
            when(authServiceClient.getCustomerProfile(CUSTOMER_ID))
                    .thenReturn(new com.smartSure.PolicyService.dto.client.CustomerProfileResponse(CUSTOMER_ID, "Test", "customer@email.com", "1234567890", 30));

            // Act
            policyService.purchasePolicy(CUSTOMER_ID, request);

            // Assert — saveAll should be called with exactly 12 premiums
            ArgumentCaptor<List<Premium>> premiumsCaptor = ArgumentCaptor.forClass(List.class);
            verify(premiumRepository).saveAll(premiumsCaptor.capture());
            assertThat(premiumsCaptor.getValue()).hasSize(12);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getPolicyById
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPolicyById()")
    class GetPolicyByIdTests {

        @Test
        @DisplayName("should return policy when customer owns it")
        void getPolicyById_ownerAccess_returnsPolicy() {
            // Arrange
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByPolicyId(POLICY_ID)).thenReturn(List.of());
            when(policyMapper.toResponseWithPremiums(eq(savedPolicy), anyList()))
                    .thenReturn(policyResponse);

            // Act
            PolicyResponse result = policyService.getPolicyById(POLICY_ID, CUSTOMER_ID, false);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(POLICY_ID);
        }

        @Test
        @DisplayName("should return policy when caller is admin (different userId)")
        void getPolicyById_adminAccess_returnsPolicy() {
            // Arrange
            Long adminId = 999L;
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByPolicyId(POLICY_ID)).thenReturn(List.of());
            when(policyMapper.toResponseWithPremiums(eq(savedPolicy), anyList()))
                    .thenReturn(policyResponse);

            // Act — isAdmin = true bypasses ownership check
            PolicyResponse result = policyService.getPolicyById(POLICY_ID, adminId, true);

            // Assert
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should throw UnauthorizedAccessException when customer accesses another's policy")
        void getPolicyById_wrongCustomer_throwsUnauthorized() {
            // Arrange
            Long otherCustomerId = 999L;
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            // savedPolicy.customerId = 42, but caller is 999

            // Act & Assert
            assertThatThrownBy(() ->
                    policyService.getPolicyById(POLICY_ID, otherCustomerId, false))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("should throw PolicyNotFoundException when policy does not exist")
        void getPolicyById_notFound_throwsException() {
            // Arrange
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() ->
                    policyService.getPolicyById(POLICY_ID, CUSTOMER_ID, false))
                    .isInstanceOf(PolicyNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // cancelPolicy
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cancelPolicy()")
    class CancelPolicyTests {

        @Test
        @DisplayName("should cancel active policy and waive pending premiums")
        void cancelPolicy_activePolicy_cancelsSuccessfully() {
            // Arrange
            Premium pendingPremium = Premium.builder()
                    .id(1L).policy(savedPolicy)
                    .amount(new BigDecimal("2625.00"))
                    .dueDate(LocalDate.now().plusMonths(1))
                    .status(Premium.PremiumStatus.PENDING)
                    .build();

            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByPolicyIdAndStatus(POLICY_ID, Premium.PremiumStatus.PENDING))
                    .thenReturn(List.of(pendingPremium));
            when(policyRepository.save(any())).thenReturn(savedPolicy);
            when(policyMapper.toResponse(savedPolicy)).thenReturn(policyResponse);
            when(authServiceClient.getCustomerProfile(CUSTOMER_ID)).thenReturn(new com.smartSure.PolicyService.dto.client.CustomerProfileResponse(CUSTOMER_ID, "Test", "c@email.com", "1234567890", 30));

            // Act
            PolicyResponse result = policyService.cancelPolicy(POLICY_ID, CUSTOMER_ID, "No longer needed");

            // Assert
            assertThat(result).isNotNull();
            // Pending premium must be waived
            assertThat(pendingPremium.getStatus()).isEqualTo(Premium.PremiumStatus.WAIVED);
            verify(policyRepository, times(1)).save(any());
            verify(auditLogRepository, times(1)).save(any(AuditLog.class));
            verify(notificationPublisher, times(1)).publishPolicyCancelled(any());
        }

        @Test
        @DisplayName("should throw UnauthorizedAccessException when wrong customer tries to cancel")
        void cancelPolicy_wrongCustomer_throwsUnauthorized() {
            // Arrange
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            // savedPolicy owner = 42, attacker = 999

            // Act & Assert
            assertThatThrownBy(() ->
                    policyService.cancelPolicy(POLICY_ID, 999L, "reason"))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when policy is already cancelled")
        void cancelPolicy_alreadyCancelled_throwsException() {
            // Arrange
            savedPolicy.setStatus(Policy.PolicyStatus.CANCELLED);
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));

            // Act & Assert
            assertThatThrownBy(() ->
                    policyService.cancelPolicy(POLICY_ID, CUSTOMER_ID, "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("should throw IllegalStateException when trying to cancel an expired policy")
        void cancelPolicy_expiredPolicy_throwsException() {
            // Arrange
            savedPolicy.setStatus(Policy.PolicyStatus.EXPIRED);
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));

            // Act & Assert
            assertThatThrownBy(() ->
                    policyService.cancelPolicy(POLICY_ID, CUSTOMER_ID, "reason"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Expired policies cannot be cancelled");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // payPremium
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("payPremium()")
    class PayPremiumTests {

        private PremiumPaymentRequest buildPaymentRequest() {
            return PremiumPaymentRequest.builder()
                    .policyId(POLICY_ID)
                    .premiumId(1L)
                    .paymentMethod(Premium.PaymentMethod.UPI)
                    .paymentReference("TXN-123456")
                    .build();
        }

        @Test
        @DisplayName("should pay PENDING premium successfully")
        void payPremium_pendingPremium_paidSuccessfully() {
            // Arrange
            PremiumPaymentRequest request = buildPaymentRequest();

            Premium pendingPremium = Premium.builder()
                    .id(1L).policy(savedPolicy)
                    .amount(new BigDecimal("2625.00"))
                    .dueDate(LocalDate.now())
                    .status(Premium.PremiumStatus.PENDING)
                    .build();

            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByIdAndPolicyId(1L, POLICY_ID))
                    .thenReturn(Optional.of(pendingPremium));
            when(premiumRepository.save(any())).thenReturn(pendingPremium);
            when(authServiceClient.getCustomerProfile(CUSTOMER_ID))
                    .thenReturn(new com.smartSure.PolicyService.dto.client.CustomerProfileResponse(CUSTOMER_ID, "Test", "c@email.com", "1234567890", 30));

            // Act
            PremiumResponse result = policyService.payPremium(CUSTOMER_ID, request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(pendingPremium.getStatus()).isEqualTo(Premium.PremiumStatus.PAID);
            assertThat(pendingPremium.getPaidDate()).isEqualTo(LocalDate.now());
            assertThat(pendingPremium.getPaymentMethod()).isEqualTo(Premium.PaymentMethod.UPI);
            verify(notificationPublisher, times(1)).publishPremiumPaid(any());
            verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("should auto-generate TXN reference when none provided")
        void payPremium_noReference_autoGeneratesReference() {
            // Arrange
            PremiumPaymentRequest request = PremiumPaymentRequest.builder()
                    .policyId(POLICY_ID).premiumId(1L)
                    .paymentMethod(Premium.PaymentMethod.UPI)
                    .paymentReference(null)  // no reference provided
                    .build();

            Premium pendingPremium = Premium.builder()
                    .id(1L).policy(savedPolicy)
                    .amount(new BigDecimal("2625.00"))
                    .dueDate(LocalDate.now())
                    .status(Premium.PremiumStatus.PENDING)
                    .build();

            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByIdAndPolicyId(1L, POLICY_ID))
                    .thenReturn(Optional.of(pendingPremium));
            when(premiumRepository.save(any())).thenReturn(pendingPremium);
            when(authServiceClient.getCustomerProfile(CUSTOMER_ID))
                    .thenReturn(new com.smartSure.PolicyService.dto.client.CustomerProfileResponse(CUSTOMER_ID, "Test", "c@email.com", "1234567890", 30));

            // Act
            policyService.payPremium(CUSTOMER_ID, request);

            // Assert — reference should be auto-generated with TXN- prefix
            assertThat(pendingPremium.getPaymentReference()).startsWith("TXN-");
        }

        @Test
        @DisplayName("should throw IllegalStateException when premium is already PAID")
        void payPremium_alreadyPaid_throwsException() {
            // Arrange
            PremiumPaymentRequest request = buildPaymentRequest();

            Premium paidPremium = Premium.builder()
                    .id(1L).policy(savedPolicy)
                    .status(Premium.PremiumStatus.PAID)
                    .build();

            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByIdAndPolicyId(1L, POLICY_ID))
                    .thenReturn(Optional.of(paidPremium));

            // Act & Assert
            assertThatThrownBy(() -> policyService.payPremium(CUSTOMER_ID, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already paid");
        }

        @Test
        @DisplayName("should throw IllegalStateException when premium is WAIVED")
        void payPremium_waivedPremium_throwsException() {
            // Arrange
            PremiumPaymentRequest request = buildPaymentRequest();

            Premium waivedPremium = Premium.builder()
                    .id(1L).policy(savedPolicy)
                    .status(Premium.PremiumStatus.WAIVED)
                    .build();

            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByIdAndPolicyId(1L, POLICY_ID))
                    .thenReturn(Optional.of(waivedPremium));

            // Act & Assert
            assertThatThrownBy(() -> policyService.payPremium(CUSTOMER_ID, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Waived premiums cannot be paid");
        }

        @Test
        @DisplayName("should throw UnauthorizedAccessException when wrong customer pays")
        void payPremium_wrongCustomer_throwsUnauthorized() {
            // Arrange
            PremiumPaymentRequest request = buildPaymentRequest();
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            // savedPolicy.customerId = 42, attacker = 777

            // Act & Assert
            assertThatThrownBy(() -> policyService.payPremium(777L, request))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }

        @Test
        @DisplayName("should throw PremiumNotFoundException when premium not found for policy")
        void payPremium_premiumNotFound_throwsException() {
            // Arrange
            PremiumPaymentRequest request = buildPaymentRequest();
            when(policyRepository.findById(POLICY_ID)).thenReturn(Optional.of(savedPolicy));
            when(premiumRepository.findByIdAndPolicyId(1L, POLICY_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> policyService.payPremium(CUSTOMER_ID, request))
                    .isInstanceOf(PremiumNotFoundException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getPolicySummary
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPolicySummary()")
    class GetPolicySummaryTests {

        @Test
        @DisplayName("should return correct summary with all counts and totals")
        void getPolicySummary_returnsSummary() {
            // Arrange
            when(policyRepository.count()).thenReturn(100L);
            when(policyRepository.countByStatus(Policy.PolicyStatus.ACTIVE)).thenReturn(70L);
            when(policyRepository.countByStatus(Policy.PolicyStatus.EXPIRED)).thenReturn(20L);
            when(policyRepository.countByStatus(Policy.PolicyStatus.CANCELLED)).thenReturn(10L);
            when(premiumRepository.totalPremiumCollected(Premium.PremiumStatus.PAID))
                    .thenReturn(new BigDecimal("500000.00"));
            when(policyRepository.sumActiveCoverages())
                    .thenReturn(new BigDecimal("50000000.00"));

            // Act
            PolicySummaryResponse result = policyService.getPolicySummary();

            // Assert
            assertThat(result.getTotalPolicies()).isEqualTo(100L);
            assertThat(result.getActivePolicies()).isEqualTo(70L);
            assertThat(result.getExpiredPolicies()).isEqualTo(20L);
            assertThat(result.getCancelledPolicies()).isEqualTo(10L);
            assertThat(result.getTotalPremiumCollected())
                    .isEqualByComparingTo(new BigDecimal("500000.00"));
            assertThat(result.getTotalCoverageProvided())
                    .isEqualByComparingTo(new BigDecimal("50000000.00"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getCustomerPolicies (pagination)
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCustomerPolicies()")
    class GetCustomerPoliciesTests {

        @Test
        @DisplayName("should return paginated policies for customer")
        void getCustomerPolicies_returnsPageResponse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Policy> page = new PageImpl<>(
                    List.of(savedPolicy), pageable, 1L);

            when(policyRepository.findByCustomerId(CUSTOMER_ID, pageable)).thenReturn(page);
            when(policyMapper.toResponse(savedPolicy)).thenReturn(policyResponse);

            // Act
            PolicyPageResponse result = policyService.getCustomerPolicies(CUSTOMER_ID, pageable);

            // Assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getPageNumber()).isEqualTo(0);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.isLast()).isTrue();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Schedulers
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Schedulers")
    class SchedulerTests {

        @Test
        @DisplayName("expirePolicies() should mark expired policies as EXPIRED")
        void expirePolicies_marksExpiredPolicies() {
            // Arrange
            Policy expiredPolicy = Policy.builder()
                    .id(5L).customerId(CUSTOMER_ID)
                    .status(Policy.PolicyStatus.ACTIVE)
                    .endDate(LocalDate.now().minusDays(1))
                    .build();

            when(policyRepository.findExpiredActivePolicies(
                    eq(Policy.PolicyStatus.ACTIVE), any(LocalDate.class)))
                    .thenReturn(List.of(expiredPolicy));

            // Act
            policyService.expirePolicies();

            // Assert
            assertThat(expiredPolicy.getStatus()).isEqualTo(Policy.PolicyStatus.EXPIRED);
            verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("markOverduePremiums() should mark overdue premiums as OVERDUE")
        void markOverduePremiums_marksOverduePremiums() {
            // Arrange
            Premium overduePremium = Premium.builder()
                    .id(3L)
                    .dueDate(LocalDate.now().minusDays(5))
                    .status(Premium.PremiumStatus.PENDING)
                    .build();

            when(premiumRepository.findOverduePremiums(
                    Premium.PremiumStatus.PENDING, LocalDate.now()))
                    .thenReturn(List.of(overduePremium));

            // Act
            policyService.markOverduePremiums();

            // Assert
            assertThat(overduePremium.getStatus()).isEqualTo(Premium.PremiumStatus.OVERDUE);
        }

        @Test
        @DisplayName("expirePolicies() should do nothing when no expired policies exist")
        void expirePolicies_noPolicies_doesNothing() {
            // Arrange
            when(policyRepository.findExpiredActivePolicies(any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            policyService.expirePolicies();

            // Assert
            verify(auditLogRepository, never()).save(any());
        }
    }
}
