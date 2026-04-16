package com.smartSure.adminService.messaging;

import com.smartSure.adminService.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = RabbitMQConfig.ADMIN_CLAIM_AUDIT_QUEUE)
    public void handleClaimDecision(ClaimDecisionEvent event) {
        log.info("Audit: ClaimDecision received — claimId={}, decision={}",
                event.getClaimId(), event.getDecision());

        String details = "customer: " + event.getCustomerEmail()
                + (event.getRemarks() != null ? " | remarks: " + event.getRemarks() : "");

        auditLogService.log(
                0L,
                event.getDecision() + "_CLAIM",
                "Claim",
                event.getClaimId(),
                details
        );
    }

    // Auto-log payment completions
    @RabbitListener(queues = RabbitMQConfig.ADMIN_PAYMENT_AUDIT_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Audit: PaymentCompleted received — premiumId={}, policyId={}", event.getPremiumId(), event.getPolicyId());
        auditLogService.log(0L, "PAYMENT_COMPLETED", "Policy", event.getPolicyId(),
                "Premium " + event.getPremiumId() + " paid — amount: " + event.getAmount());
    }
}
