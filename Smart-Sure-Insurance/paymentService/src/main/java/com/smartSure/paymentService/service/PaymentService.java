package com.smartSure.paymentService.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.smartSure.paymentService.config.RabbitMQConfig;
import com.smartSure.paymentService.dto.*;
import com.smartSure.paymentService.entity.Payment;
import com.smartSure.paymentService.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RazorpayClient razorpayClient;
    @Value("${razorpay.key.secret}")
    private String razorpaySecret;


    /**
     * Mock payment simulator — no Razorpay or any third-party gateway.
     *
     * Flow:
     *  1. Validate the request.
     *  2. Generate a mock transaction reference ID.
     *  3. Randomly decide SUCCESS (80%) or FAILED (20%).
     *     Client can override by passing forceOutcome = "SUCCESS" | "FAILED".
     *  4. Persist the result.
     *  5. On SUCCESS, publish PaymentCompletedEvent to RabbitMQ so PolicyService
     *     updates the premium status — same as the real Razorpay confirm path.
     */
    @Transactional
    @CacheEvict(value = {"payments", "customerPayments"}, allEntries = true)
    public PaymentResponse simulatePayment(Long customerId, SimulatePaymentRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (request.getPolicyId() == null || request.getPremiumId() == null) {
            throw new IllegalArgumentException("PolicyId and PremiumId are required");
        }

        // Generate mock references — format similar to Razorpay IDs so existing
        // DB columns and response fields stay compatible.
        String mockOrderId  = "mock_ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String mockPayId    = "mock_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // Determine outcome
        boolean succeed;
        if ("SUCCESS".equalsIgnoreCase(request.getForceOutcome())) {
            succeed = true;
        } else if ("FAILED".equalsIgnoreCase(request.getForceOutcome())) {
            succeed = false;
        } else {
            // 80% success rate by default
            succeed = new Random().nextInt(100) < 80;
        }

        Payment.PaymentStatus status = succeed
                ? Payment.PaymentStatus.SUCCESS
                : Payment.PaymentStatus.FAILED;

        Payment payment = Payment.builder()
                .policyId(request.getPolicyId())
                .premiumId(request.getPremiumId())
                .customerId(customerId)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(status)
                .razorpayOrderId(mockOrderId)
                .razorpayPaymentId(succeed ? mockPayId : null)
                .failureReason(succeed ? null : "Payment declined by issuing bank (simulated)")
                .updatedAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        log.info("[SIM] Payment {} — policyId={}, premiumId={}, orderId={}",
                status, request.getPolicyId(), request.getPremiumId(), mockOrderId);

        if (succeed) {
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(saved.getId())
                    .policyId(saved.getPolicyId())
                    .premiumId(saved.getPremiumId())
                    .customerId(saved.getCustomerId())
                    .amount(saved.getAmount())
                    .paymentMethod(saved.getPaymentMethod() != null ? saved.getPaymentMethod().name() : null)
                    .razorpayPaymentId(mockPayId)
                    .paidAt(LocalDateTime.now())
                    .build();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.EXCHANGE,
                            RabbitMQConfig.PAYMENT_COMPLETED_KEY,
                            event
                    );
                    log.info("[SIM] PaymentCompletedEvent published AFTER COMMIT — premiumId={}", saved.getPremiumId());
                }
            });
        }

        return toResponse(saved, null);
    }

    public PaymentResponse initiatePayment(Long customerId, PaymentRequest request) {
        try {
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than zero");
            }

            if (request.getPolicyId() == null || request.getPremiumId() == null) {
                throw new IllegalArgumentException("PolicyId and PremiumId are required");
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_pol" + request.getPolicyId() + "_pre" + request.getPremiumId());

            Order order = razorpayClient.orders.create(orderRequest);

            Payment payment = Payment.builder()
                    .policyId(request.getPolicyId())
                    .premiumId(request.getPremiumId())
                    .customerId(customerId)
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .status(Payment.PaymentStatus.PENDING)
                    .razorpayOrderId(order.get("id"))
                    .build();

            Payment saved = paymentRepository.save(payment);

            log.info("Payment initiated — policyId={}, premiumId={}, orderId={}",
                    request.getPolicyId(), request.getPremiumId(), order.get("id"));

            return toResponse(saved, null);

        } catch (Exception e) {
            log.error("Payment initiation failed: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    // Confirm payment after Razorpay success callback — marks SUCCESS, publishes RabbitMQ event
    // FIX: Added HMAC validation to prevent spoofing


    @Transactional
    @CacheEvict(value = {"payments", "customerPayments"}, allEntries = true)
    public PaymentResponse confirmPayment(ConfirmPaymentRequest req) {

        verifyRazorpaySignature(
                req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(),
                req.getRazorpaySignature()
        );

        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + req.getRazorpayOrderId()));

        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment already processed");
        }

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        //  Prepare event (but DON'T send yet)
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(saved.getId())
                .policyId(saved.getPolicyId())
                .premiumId(saved.getPremiumId())
                .customerId(saved.getCustomerId())
                .amount(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod() != null ? saved.getPaymentMethod().name() : null)
                .razorpayPaymentId(req.getRazorpayPaymentId())
                .paidAt(LocalDateTime.now())
                .build();

        //  Publish AFTER transaction commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.PAYMENT_COMPLETED_KEY,
                        event
                );
                log.info("PaymentCompletedEvent published AFTER COMMIT — premiumId={}", saved.getPremiumId());
            }
        });

        return toResponse(saved, null);
    }

    private void verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");

            mac.init(new javax.crypto.spec.SecretKeySpec(
                    razorpaySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] hash = mac.doFinal(
                    payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            if (!hexString.toString().equals(signature)) {
                throw new RuntimeException("Invalid Razorpay signature. Potential spoofing attempt!");
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Razorpay signature: " + e.getMessage());
        }
    }

    // Refund payment
    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only successful payments can be refunded");
        }
        
        // In a real scenario, we'd make a Razorpay refund API call here:
        // razorpay.payments.refund(refundRequest)

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment), null);
    }

    // Saga: Pay out claim to customer when ClaimService publishes APPROVED decision
    @RabbitListener(queues = RabbitMQConfig.CLAIM_PAYOUT_QUEUE)
    public void processClaimPayout(ClaimDecisionEvent event) {

        log.info("Received ClaimDecisionEvent for claimId={}, decision={}",
                event.getClaimId(), event.getDecision());

        if (!"APPROVED".equals(event.getDecision())) {
            log.info("Claim {} not approved. Skipping payout.", event.getClaimId());
            return;
        }

        try {

            Payment payout = Payment.builder()
                    .policyId(event.getPolicyId())
                    .premiumId(0L) // Set to 0 to bypass legacy NOT NULL constraint for payouts
                    .customerId(event.getCustomerId()) //   use from ClaimDecisionEvent
                    .amount(event.getAmount())
                    .paymentMethod(Payment.PaymentMethod.UPI)
                    .status(Payment.PaymentStatus.SUCCESS)
                    .razorpayPaymentId("PAYOUT_CLAIM_" + event.getClaimId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payout);

            log.info("Payout successful for claimId={}, amount={}",
                    event.getClaimId(), event.getAmount());

        } catch (Exception e) {
            log.error("Payout failed for claimId={}: {}", event.getClaimId(), e.getMessage());
            throw new RuntimeException("Payout processing failed", e);
        }
    }

    // Mark payment as failed
    public PaymentResponse failPayment(FailPaymentRequest req) {
        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + req.getRazorpayOrderId()));
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason(req.getReason());
        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment), null);
    }

    // Cache payment by ID — payment data is immutable once SUCCESS
    @Cacheable(value = "payments", key = "#id")
    public PaymentResponse getPaymentById(Long id) {
        return toResponse(paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id)), null);
    }

    // Always read fresh — cache caused stale 0 count on customer dashboard
    public List<PaymentResponse> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(p -> toResponse(p, null)).toList();
    }

    public List<PaymentResponse> getPaymentsByPolicy(Long policyId) {
        return paymentRepository.findByPolicyId(policyId).stream()
                .map(p -> toResponse(p, null)).toList();
    }
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> toResponse(p, null)).toList();
    }
    private PaymentResponse toResponse(Payment p, String keyId) {
        return PaymentResponse.builder()
                .id(p.getId())
                .policyId(p.getPolicyId())
                .premiumId(p.getPremiumId())
                .customerId(p.getCustomerId())
                .amount(p.getAmount())
                .status(p.getStatus().name())
                .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .razorpayKeyId(keyId)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
