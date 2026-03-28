package com.smartSure.paymentService.dto;

import com.smartSure.paymentService.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long policyId;
    private Long premiumId;
    private Long customerId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpayKeyId;   // returned on /initiate so frontend can open Razorpay checkout
    private LocalDateTime createdAt;
}
