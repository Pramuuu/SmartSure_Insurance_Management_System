package com.smartSure.paymentService.dto;

import com.smartSure.paymentService.entity.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PaymentRequest {
    private Long policyId;
    private Long premiumId;
    private BigDecimal amount;
    private Payment.PaymentMethod paymentMethod;
}
