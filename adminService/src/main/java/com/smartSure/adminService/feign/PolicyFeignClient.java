package com.smartSure.adminService.feign;

import com.smartSure.adminService.dto.PolicyDTO;
import com.smartSure.adminService.dto.PolicyStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "policyservice")
public interface PolicyFeignClient {

    /**
     * FIX: Changed from /api/policies/admin/all to /api/policies/admin/list
     *
     * The old endpoint /api/policies/admin/all returns PolicyPageResponse:
     *   { "content": [...], "pageNumber": 0, "totalElements": N, ... }
     *
     * Feign tried to deserialize that object as List<PolicyDTO> and threw a
     * deserialization exception. This broke:
     *   - Admin dashboard (policy count, policy stats)
     *   - Admin policies page
     *   - Admin user detail → policies tab
     *
     * The new /api/policies/admin/list endpoint (added to PolicyController)
     * returns List<PolicyResponse> directly — no pagination wrapper.
     */
    @GetMapping("/api/policies/admin/list")
    List<PolicyDTO> getAllPolicies();

    @GetMapping("/api/policies/{policyId}")
    PolicyDTO getPolicyById(@PathVariable Long policyId);

    @PutMapping("/api/policies/admin/{policyId}/status")
    PolicyDTO updatePolicyStatus(
            @PathVariable Long policyId,
            @RequestBody PolicyStatusUpdateRequest request
    );

    @GetMapping("/api/policies/admin/summary")
    Object getPolicySummary();
}