package com.smartSure.paymentService.dto;

import com.smartSure.paymentService.entity.Payment;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for the mock payment simulator endpoint.
 * Replaces the Razorpay initiate → confirm 2-step flow with a single
 * self-contained call that randomly succeeds or fails.
 */
@Data
@NoArgsConstructor
public class SimulatePaymentRequest {
    private Long policyId;
    private Long premiumId;
    private BigDecimal amount;
    private Payment.PaymentMethod paymentMethod;

    /**
     * Optional: client can force a specific outcome for testing.
     * Values: "SUCCESS", "FAILED" — if null, the server decides randomly (80% success).
     */
    private String forceOutcome;
}
