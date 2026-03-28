package com.smartSure.claimService.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PolicyDTO {
    private Long id;
    private Long customerId;
    private BigDecimal coverageAmount;
}