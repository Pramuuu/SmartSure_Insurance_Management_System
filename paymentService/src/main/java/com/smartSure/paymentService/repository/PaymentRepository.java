package com.smartSure.paymentService.repository;

import com.smartSure.paymentService.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCustomerId(Long customerId);
    List<Payment> findByPolicyId(Long policyId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
