package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusUpdateRequest {

    // FIXED: was "status" — must match ClaimService's StatusUpdateRequest.nextStatus
    private String nextStatus;
    private String remarks;
}