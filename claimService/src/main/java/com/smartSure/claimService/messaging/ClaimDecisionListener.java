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
        log.info("Received ClaimDecisionEvent — claimId={}, decision={}", event.getClaimId(), event.getDecision());
        try {
            emailService.sendClaimDecisionEmail(
                    event.getCustomerEmail(),
                    event.getCustomerName(),
                    event.getClaimId(),
                    event.getDecision()
            );
        } catch (Exception e) {
            log.error("Failed to send decision email for claim {}: {}", event.getClaimId(), e.getMessage());
        }
    }
}
