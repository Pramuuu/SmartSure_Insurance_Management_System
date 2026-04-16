package com.smartSure.adminService.feign;

import com.smartSure.adminService.dto.PaymentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for PaymentService.
 * Used by AdminService to view payment history — currently
 * AdminService had zero visibility into payments.
 */
@FeignClient(name = "paymentservice")
public interface PaymentFeignClient {

    // Get all payments — full admin view
    @GetMapping("/api/payments/all")
    List<PaymentDTO> getAllPayments();

    // Get single payment by ID
    @GetMapping("/api/payments/{id}")
    PaymentDTO getPaymentById(@PathVariable Long id);

    // Get all payments for a specific customer
    @GetMapping("/api/payments/customer/{customerId}")
    List<PaymentDTO> getPaymentsByCustomer(@PathVariable Long customerId);

    // Get all payments for a specific policy
    @GetMapping("/api/payments/policy/{policyId}")
    List<PaymentDTO> getPaymentsByPolicy(@PathVariable Long policyId);
}