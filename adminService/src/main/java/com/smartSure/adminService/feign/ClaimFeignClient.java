package com.smartSure.adminService.feign;

import com.smartSure.adminService.dto.ClaimDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "claimservice")
public interface ClaimFeignClient {

    // Fetch all claims — admin full list
    @GetMapping("/api/claims")
    List<ClaimDTO> getAllClaims();

    // Fetch claims that are UNDER_REVIEW — admin review queue
    @GetMapping("/api/claims/under-review")
    List<ClaimDTO> getUnderReviewClaims();

    // Fetch a single claim by ID
    @GetMapping("/api/claims/{id}")
    ClaimDTO getClaimById(@PathVariable Long id);

    // Move claim to next status (APPROVED / REJECTED)
    @PutMapping("/api/claims/{id}/status")
    ClaimDTO updateClaimStatus(@PathVariable Long id, @RequestParam("next") String status);
}
