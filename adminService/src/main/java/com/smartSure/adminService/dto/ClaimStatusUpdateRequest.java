package com.smartSure.adminService.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusUpdateRequest {
    private String status;   // APPROVED or REJECTED
    private String remarks;
}
