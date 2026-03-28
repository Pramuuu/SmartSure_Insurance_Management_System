package com.smartSure.adminService.feign;

import com.smartSure.adminService.dto.PolicyDTO;
import com.smartSure.adminService.dto.PolicyStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "POLICYSERVICE")
public interface PolicyFeignClient {

    // GET /api/policies/admin/all — no headers needed, ADMIN role via JWT
    @GetMapping("/api/policies/admin/all")
    List<PolicyDTO> getAllPolicies();

    // GET /api/policies/{policyId} — requires X-User-Id and X-User-Role headers
    // Passing ADMIN role so policyService skips ownership check
    @GetMapping("/api/policies/{policyId}")
    PolicyDTO getPolicyById(
            @PathVariable Long policyId
    );

    // PUT /api/policies/admin/{policyId}/status — correct admin cancel/status endpoint
    @PutMapping("/api/policies/admin/{policyId}/status")
    PolicyDTO updatePolicyStatus(
            @PathVariable Long policyId,
            @RequestBody PolicyStatusUpdateRequest request
    );
}
