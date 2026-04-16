package com.smartSure.adminService.feign;

import com.smartSure.adminService.dto.ClaimDTO;
import com.smartSure.adminService.dto.ClaimStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "claimservice")
public interface ClaimFeignClient {

    // All claims — full admin view
    @GetMapping("/api/claims")
    List<ClaimDTO> getAllClaims();

    // Claims in SUBMITTED + UNDER_REVIEW — admin work queue
    @GetMapping("/api/claims/under-review")
    List<ClaimDTO> getUnderReviewClaims();

    // Single claim by ID
    @GetMapping("/api/claims/{id}")
    ClaimDTO getClaimById(@PathVariable Long id);

    // Move claim to next status with remarks
    // FIXED: was @RequestParam("next") String status
    @PutMapping("/api/claims/{id}/status")
    ClaimDTO updateClaimStatus(
            @PathVariable Long id,
            @RequestBody ClaimStatusUpdateRequest request
    );

    // Get all claims for a specific customer — for admin user detail view
    @GetMapping("/api/claims/customer/{userId}")
    List<ClaimDTO> getClaimsByUser(@PathVariable Long userId);
}