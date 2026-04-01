package com.smartSure.paymentService.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.smartSure.paymentService.config.RabbitMQConfig;
import com.smartSure.paymentService.dto.ConfirmPaymentRequest;
import com.smartSure.paymentService.dto.FailPaymentRequest;
import com.smartSure.paymentService.dto.PaymentCompletedEvent;
import com.smartSure.paymentService.dto.PaymentRequest;
import com.smartSure.paymentService.dto.PaymentResponse;
import com.smartSure.paymentService.entity.Payment;
import com.smartSure.paymentService.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // Initiate payment — creates Razorpay order, saves PENDING record
    // Returns razorpayKeyId + razorpayOrderId so frontend can open Razorpay checkout
    public PaymentResponse initiatePayment(Long customerId, PaymentRequest request) {
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_pol" + request.getPolicyId() + "_pre" + request.getPremiumId());

            Order order = razorpay.orders.create(orderRequest);

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

            return toResponse(saved, razorpayKeyId);

        } catch (Exception e) {
            log.error("Payment initiation failed: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    // Confirm payment after Razorpay success callback — marks SUCCESS, publishes RabbitMQ event
    // FIX: Added HMAC validation to prevent spoofing
    public PaymentResponse confirmPayment(ConfirmPaymentRequest req) {
        verifyRazorpaySignature(req.getRazorpayOrderId(), req.getRazorpayPaymentId(), req.getRazorpaySignature());

        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + req.getRazorpayOrderId()));

        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment already processed");
        }

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setUpdatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

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

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.PAYMENT_COMPLETED_KEY, event);
        log.info("PaymentCompletedEvent published — premiumId={}", saved.getPremiumId());
        return toResponse(saved, null);
    }

    private void verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            if (!hexString.toString().equals(signature)) {
                throw new RuntimeException("Invalid Razorpay signature. Potential spoofing attempt!");
            }
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
    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = RabbitMQConfig.CLAIM_PAYOUT_QUEUE)
    public void processClaimPayout(com.smartSure.paymentService.dto.ClaimDecisionEvent event) {
        log.info("Received ClaimDecisionEvent for claimId={}, decision={}", event.getClaimId(), event.getDecision());

        if (!"APPROVED".equals(event.getDecision())) {
            log.info("Claim {} was not approved (decision: {}). Skipping payout.", event.getClaimId(), event.getDecision());
            return;
        }

        try {
            // In a real scenario, we'd make a Razorpay Payout API call here to transfer funds back
            // e.g., razorpay.payouts.create(payoutRequest)

            Payment payout = Payment.builder()
                    .policyId(event.getPolicyId())
                    .amount(event.getAmount())
                    // Assuming customer logic mapping; using null or retrieving via feign
                    // .customerId(...)
                    .paymentMethod(Payment.PaymentMethod.UPI) // default or fetch from preferred method
                    .status(Payment.PaymentStatus.SUCCESS)
                    .razorpayPaymentId("PAYOUT_" + event.getClaimId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payout);
            log.info("Successfully processed payout for claimId={}, amount={}", event.getClaimId(), event.getAmount());

        } catch (Exception e) {
            log.error("Failed to process payout for claimId={}: {}", event.getClaimId(), e.getMessage());
            // Retry logic or DLQ handled by RabbitMQ
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

    public PaymentResponse getPaymentById(Long id) {
        return toResponse(paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id)), null);
    }

    public List<PaymentResponse> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(p -> toResponse(p, null)).toList();
    }

    public List<PaymentResponse> getPaymentsByPolicy(Long policyId) {
        return paymentRepository.findByPolicyId(policyId).stream()
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
