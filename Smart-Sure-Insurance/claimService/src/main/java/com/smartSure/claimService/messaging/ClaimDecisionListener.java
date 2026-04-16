package com.smartSure.claimService.messaging;

import com.smartSure.claimService.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimDecisionListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.CLAIM_DECISION_QUEUE)
    public void handleClaimDecision(ClaimDecisionEvent event) {
        log.info("Received ClaimDecisionEvent — claimId={}, decision={}",
                event.getClaimId(), event.getDecision());

        if (event.getCustomerEmail() == null || event.getCustomerEmail().isBlank()) {
            log.warn("No customer email for claimId={} — skipping email",
                    event.getClaimId());
            return;
        }

        emailService.sendClaimDecisionEmail(
                event.getCustomerEmail(),
                event.getCustomerName(),
                event.getClaimId(),
                event.getDecision(),
                event.getRemarks()             // ← ADD THIS
        );

        log.info("Decision email sent for claimId={}", event.getClaimId());
    }
}
