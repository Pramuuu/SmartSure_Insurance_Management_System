package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDTO {

    private Long id;
    private Long policyId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String description;
    private LocalDate incidentDate;       // ← LocalDate not LocalDateTime
    private String incidentType;
    private String incidentLocation;
    private String hospitalName;
    private String treatmentType;
    private String vehicleNumber;
    private String garageRepairShop;
    private String policeReportNumber;
    private String witnessName;
    private String witnessContact;
    private boolean pdfGenerated;
    private boolean evidenceUploaded;
    private boolean aadhaarUploaded;
    private boolean consentGiven;
    private LocalDateTime consentTimestamp;
    private String adminRemarks;
    private LocalDateTime reviewedAt;
    private boolean autoApproved;
    private LocalDateTime timeOfCreation;
    private LocalDateTime lastUpdatedAt;
}