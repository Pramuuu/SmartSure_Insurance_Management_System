package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private Long id;
    private Long adminId;
    private String action;
    private String targetEntity;
    private Long targetId;
    private String remarks;
    private LocalDateTime performedAt;
}