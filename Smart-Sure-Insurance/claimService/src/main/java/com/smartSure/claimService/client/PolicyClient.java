package com.smartSure.claimService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.smartSure.claimService.dto.PolicyDTO;

@FeignClient(name = "POLICYSERVICE", path = "/api/policies")
public interface PolicyClient {

    // FeignClientInterceptor forwards X-User-Id, X-User-Role, X-Internal-Secret automatically
    @GetMapping("/{policyId}")
    PolicyDTO getPolicyById(@PathVariable("policyId") Long policyId);
}
