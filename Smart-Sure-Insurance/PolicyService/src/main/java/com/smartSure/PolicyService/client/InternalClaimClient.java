package com.smartSure.PolicyService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * Internal Feign Client to communicate with ClaimService.
 * Uses the globally registered FeignInterceptor component to forward headers.
 */
@FeignClient(name = "claimservice", path = "/api/claims")
public interface InternalClaimClient {

    @GetMapping("/internal/total-approved/{policyId}")
    BigDecimal getTotalApprovedClaimsAmount(@PathVariable("policyId") Long policyId);
}
