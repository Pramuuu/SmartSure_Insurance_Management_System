package com.smartSure.adminService.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long paymentId;
    private Long policyId;
    private Long premiumId;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String razorpayPaymentId;
    private LocalDateTime paidAt;
}
