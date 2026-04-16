package com.smartSure.PolicyService.service;

import com.smartSure.PolicyService.dto.calculation.PremiumCalculationRequest;
import com.smartSure.PolicyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.PolicyService.dto.policy.*;
import com.smartSure.PolicyService.dto.premium.PremiumPaymentRequest;
import com.smartSure.PolicyService.dto.premium.PremiumResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PolicyService {

    PolicyResponse purchasePolicy(Long customerId, PolicyPurchaseRequest request);

    PolicyPageResponse getCustomerPolicies(Long customerId, Pageable pageable);

    PolicyResponse getPolicyById(Long policyId, Long userId, boolean isAdmin);

    PolicyPageResponse getAllPolicies(Pageable pageable);

    /**
     * FIX: New method — returns all policies as a flat List with no pagination wrapper.
     * Used by the new GET /api/policies/admin/list endpoint, which is called by
     * AdminService's PolicyFeignClient. The Feign client needs a plain List, not a
     * PolicyPageResponse, to deserialize correctly.
     */
    List<PolicyResponse> getAllPoliciesAsList();

    PolicyResponse cancelPolicy(Long policyId, Long customerId, String reason);

    PolicyResponse renewPolicy(Long customerId, PolicyRenewalRequest request);

    PremiumResponse payPremium(Long customerId, PremiumPaymentRequest request);

    List<PremiumResponse> getPremiumsByPolicy(Long policyId);

    PolicyResponse adminUpdatePolicyStatus(Long policyId, PolicyStatusUpdateRequest request);

    PolicySummaryResponse getPolicySummary();

    PremiumCalculationResponse calculatePremium(PremiumCalculationRequest request);

    boolean validateCoverage(Long policyId, Long userId, boolean isAdmin);

    void expirePolicies();

    void markOverduePremiums();
}