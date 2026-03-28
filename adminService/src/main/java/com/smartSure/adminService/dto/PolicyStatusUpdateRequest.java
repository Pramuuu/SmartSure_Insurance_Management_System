package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyStatusUpdateRequest {
    private String status;  // ACTIVE, CANCELLED, EXPIRED
    private String reason;
}
