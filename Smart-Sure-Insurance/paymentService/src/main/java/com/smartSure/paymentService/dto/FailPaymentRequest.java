package com.smartSure.paymentService.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FailPaymentRequest {
    private String razorpayOrderId;
    private String reason;
}
