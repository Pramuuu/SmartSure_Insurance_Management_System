package com.smartSure.adminService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDTO {
    private Long id;
    private String policyNumber;
    private Long customerId;
    private PolicyTypeDTO policyType;
    private BigDecimal coverageAmount;
    private BigDecimal premiumAmount;
    private String paymentFrequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String cancellationReason;
}
