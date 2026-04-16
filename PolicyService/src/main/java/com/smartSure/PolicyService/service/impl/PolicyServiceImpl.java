package com.smartSure.PolicyService.service.impl;

import com.smartSure.PolicyService.client.AuthServiceClient;
import com.smartSure.PolicyService.dto.calculation.PremiumCalculationRequest;
import com.smartSure.PolicyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.PolicyService.dto.client.CustomerProfileResponse;
import com.smartSure.PolicyService.dto.event.*;
import com.smartSure.PolicyService.dto.policy.*;
import com.smartSure.PolicyService.dto.premium.PremiumPaymentRequest;
import com.smartSure.PolicyService.dto.premium.PremiumResponse;
import com.smartSure.PolicyService.entity.*;
import com.smartSure.PolicyService.exception.*;
import com.smartSure.PolicyService.mapper.PolicyMapper;
import com.smartSure.PolicyService.repository.*;
import com.smartSure.PolicyService.service.NotificationPublisher;
import com.smartSure.PolicyService.service.PolicyService;
import com.smartSure.PolicyService.service.PremiumCalculator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.smartSure.PolicyService.client.InternalClaimClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository      policyRepository;
    private final PolicyTypeRepository  policyTypeRepository;
    private final PremiumRepository     premiumRepository;
    private final AuditLogRepository    auditLogRepository;
    private final PremiumCalculator     premiumCalculator;
    private final PolicyMapper          policyMapper;
    private final NotificationPublisher notificationPublisher;
    private final AuthServiceClient     authServiceClient;
    private final InternalClaimClient   internalClaimClient;

    // ═══════════════════════════════════════════════════════════
    // PURCHASE
    // ═══════════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "policyTypeService", fallbackMethod = "purchaseFallback")
    @RateLimiter(name = "policyPurchase", fallbackMethod = "purchaseRateLimitFallback")
    @Transactional
    public PolicyResponse purchasePolicy(Long customerId, PolicyPurchaseRequest request) {

        log.info("Purchase request — customer={}, policyTypeId={}", customerId, request.getPolicyTypeId());

        PolicyType type = policyTypeRepository.findById(request.getPolicyTypeId())
                .orElseThrow(() -> new PolicyTypeNotFoundException(request.getPolicyTypeId()));

        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        if (type.getStatus() != PolicyType.PolicyTypeStatus.ACTIVE) {
            throw new InactivePolicyTypeException(type.getName());
        }

        Optional<Policy> existingPolicyOpt = policyRepository.findFirstByCustomerIdAndPolicyType_IdAndStatusIn(
                customerId, type.getId(),
                List.of(Policy.PolicyStatus.CREATED, Policy.PolicyStatus.ACTIVE));
        
        if (existingPolicyOpt.isPresent()) {
            Policy existingPolicy = existingPolicyOpt.get();
            try {
                BigDecimal totalApprovedClaims = internalClaimClient.getTotalApprovedClaimsAmount(existingPolicy.getId());
                if (totalApprovedClaims == null) totalApprovedClaims = BigDecimal.ZERO;
                
                if (totalApprovedClaims.compareTo(existingPolicy.getCoverageAmount()) < 0) {
                    // Coverage not fully exhausted -> block duplicate purchase
                    throw new DuplicatePolicyException();
                } else {
                    log.info("Customer {} allowed to repurchase policy type {}; previous policy {} coverage fully exhausted.",
                        customerId, type.getId(), existingPolicy.getId());
                }
            } catch (DuplicatePolicyException e) {
                throw e; // rethrow expected exception
            } catch (Exception e) {
                log.warn("Failed to check claims for policy {}: {}", existingPolicy.getId(), e.getMessage());
                // Fail safe: block purchase if ClaimService is down or check fails
                throw new DuplicatePolicyException(); 
            }
        }

        if (request.getCoverageAmount().compareTo(type.getMaxCoverageAmount()) > 0) {
            throw new CoverageExceedsLimitException(request.getCoverageAmount(), type.getMaxCoverageAmount());
        }

        BigDecimal premiumAmount = premiumCalculator.calculatePremium(
                type, request.getCoverageAmount(),
                request.getPaymentFrequency(), request.getCustomerAge()
        ).getCalculatedPremium();

        Policy policy = policyMapper.toEntity(request);
        policy.setCustomerId(customerId);
        policy.setPolicyType(type);
        policy.setPremiumAmount(premiumAmount);
        policy.setPolicyNumber(generatePolicyNumber());
        policy.setEndDate(request.getStartDate().plusMonths(type.getTermMonths()));
        
        // Improvement: All policies begin as CREATED until the first premium is officially PAID.
        policy.setStatus(Policy.PolicyStatus.CREATED);

        Policy saved = policyRepository.save(policy);
        generatePremiumSchedule(saved, type.getTermMonths());
        saveAudit(saved.getId(), customerId, "CUSTOMER", "PURCHASED",
                null, saved.getStatus().name(), "New policy purchased");
        CustomerProfileResponse profile = getCustomerProfileSafely(customerId);
        notificationPublisher.publishPolicyPurchased(
                PolicyPurchasedEvent.builder()
                        .policyId(saved.getId())
                        .policyNumber(saved.getPolicyNumber())
                        .customerId(customerId)
                        .customerEmail(profile.getEmail())
                        .customerName(profile.getName())
                        .policyTypeName(type.getName())
                        .coverageAmount(saved.getCoverageAmount())
                        .premiumAmount(saved.getPremiumAmount())
                        .paymentFrequency(saved.getPaymentFrequency().name())
                        .startDate(saved.getStartDate())
                        .endDate(saved.getEndDate())
                        .status(saved.getStatus().name())
                        .nomineeName(saved.getNomineeName())
                        .build());

        log.info("Policy created — policyId={}, policyNumber={}", saved.getId(), saved.getPolicyNumber());
        return buildDetailedResponse(saved);
    }

    public PolicyResponse purchaseFallback(Long customerId, PolicyPurchaseRequest request, io.github.resilience4j.circuitbreaker.CallNotPermittedException t) {
        log.error("purchasePolicy CIRCUIT BREAKER fallback — customerId={}, reason={}", customerId, t.getMessage());
        throw new ServiceUnavailableException("Policy purchase service", t);
    }

    public PolicyResponse purchaseRateLimitFallback(Long customerId, PolicyPurchaseRequest request, io.github.resilience4j.ratelimiter.RequestNotPermitted t) {
        log.warn("purchasePolicy RATE LIMIT fallback — customerId={}", customerId);
        throw new ServiceUnavailableException("Too many purchase requests. Please wait a moment and try again.");
    }

    // ═══════════════════════════════════════════════════════════
    // GET — paginated
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PolicyPageResponse getCustomerPolicies(Long customerId, Pageable pageable) {
        Page<Policy> page = policyRepository.findByCustomerId(customerId, pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(Long policyId, Long userId, boolean isAdmin) {
        Policy policy = getPolicy(policyId);
        if (!isAdmin && !policy.getCustomerId().equals(userId)) {
            throw new UnauthorizedAccessException();
        }
        return buildDetailedResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyPageResponse getAllPolicies(Pageable pageable) {
        Page<Policy> page = policyRepository.findAll(pageable);
        return toPageResponse(page);
    }

    /**
     * FIX: New method — returns all policies as a flat List with no pagination wrapper.
     * Called by GET /api/policies/admin/list which is used by AdminService Feign client.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponse> getAllPoliciesAsList() {
        return policyRepository.findAll()
                .stream()
                .map(this::buildDetailedResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // CANCEL
    // ═══════════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "policyTypeService", fallbackMethod = "cancelFallback")
    @Transactional
    public PolicyResponse cancelPolicy(Long policyId, Long customerId, String reason) {

        Policy policy = getPolicy(policyId);

        if (!policy.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You can only cancel your own policies");
        }
        if (policy.getStatus() == Policy.PolicyStatus.CANCELLED) {
            throw new IllegalStateException("Policy is already cancelled");
        }
        if (policy.getStatus() == Policy.PolicyStatus.EXPIRED) {
            throw new IllegalStateException("Expired policies cannot be cancelled");
        }

        String prevStatus = policy.getStatus().name();
        policy.setStatus(Policy.PolicyStatus.CANCELLED);
        policy.setCancellationReason(reason);

        premiumRepository.findByPolicyIdAndStatus(policyId, Premium.PremiumStatus.PENDING)
                .forEach(p -> p.setStatus(Premium.PremiumStatus.WAIVED));

        Policy saved = policyRepository.save(policy);
        saveAudit(policyId, customerId, "CUSTOMER", "CANCELLED",
                prevStatus, Policy.PolicyStatus.CANCELLED.name(), reason);
        CustomerProfileResponse profile = getCustomerProfileSafely(customerId);
        notificationPublisher.publishPolicyCancelled(
                PolicyCancelledEvent.builder()
                        .policyId(saved.getId())
                        .policyNumber(saved.getPolicyNumber())
                        .customerId(customerId)
                        .customerEmail(profile.getEmail())
                        .customerName(profile.getName())
                        .cancellationReason(reason)
                        .build());

        return policyMapper.toResponse(saved);
    }

    public PolicyResponse cancelFallback(Long policyId, Long customerId, String reason, Throwable t) {
        log.error("cancelPolicy CIRCUIT BREAKER fallback — policyId={}, reason={}", policyId, t.getMessage());
        throw new ServiceUnavailableException("Policy cancellation service", t);
    }

    // ═══════════════════════════════════════════════════════════
    // RENEW
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public PolicyResponse renewPolicy(Long customerId, PolicyRenewalRequest request) {

        Policy oldPolicy = getPolicy(request.getPolicyId());

        if (!oldPolicy.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You can only renew your own policies");
        }

        if (oldPolicy.getStatus() == Policy.PolicyStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled policies cannot be renewed");
        }

        if (oldPolicy.getStatus() == Policy.PolicyStatus.CREATED) {
            throw new IllegalStateException("Policy has not yet become active");
        }

        oldPolicy.setStatus(Policy.PolicyStatus.EXPIRED);

        PolicyType type = oldPolicy.getPolicyType();

        BigDecimal coverage = request.getNewCoverageAmount() != null
                ? request.getNewCoverageAmount()
                : oldPolicy.getCoverageAmount();

        if (coverage.compareTo(type.getMaxCoverageAmount()) > 0) {
            throw new CoverageExceedsLimitException(coverage, type.getMaxCoverageAmount());
        }

        Policy.PaymentFrequency freq = request.getPaymentFrequency() != null
                ? request.getPaymentFrequency()
                : oldPolicy.getPaymentFrequency();

        BigDecimal premium = premiumCalculator
                .calculatePremium(type, coverage, freq, null)
                .getCalculatedPremium();

        Policy newPolicy = Policy.builder()
                .policyNumber(generatePolicyNumber())
                .customerId(customerId)
                .policyType(type)
                .coverageAmount(coverage)
                .premiumAmount(premium)
                .paymentFrequency(freq)
                .startDate(oldPolicy.getEndDate())
                .endDate(request.getNewEndDate())
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        Policy saved = policyRepository.save(newPolicy);
        generatePremiumSchedule(saved, type.getTermMonths());
        saveAudit(saved.getId(), customerId, "CUSTOMER", "RENEWED",
                null, Policy.PolicyStatus.ACTIVE.name(),
                "Renewed from policy " + oldPolicy.getPolicyNumber());
        CustomerProfileResponse profile = getCustomerProfileSafely(customerId);

        notificationPublisher.publishPolicyPurchased(
                PolicyPurchasedEvent.builder()
                        .policyId(saved.getId())
                        .policyNumber(saved.getPolicyNumber())
                        .customerId(customerId)
                        .customerEmail(profile.getEmail())
                        .customerName(profile.getName())
                        .policyTypeName(type.getName())
                        .coverageAmount(saved.getCoverageAmount())
                        .premiumAmount(saved.getPremiumAmount())
                        .paymentFrequency(saved.getPaymentFrequency().name())
                        .startDate(saved.getStartDate())
                        .endDate(saved.getEndDate())
                        .status(saved.getStatus().name())
                        .nomineeName(saved.getNomineeName())
                        .build());

        return buildDetailedResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════
    // PREMIUM PAYMENT
    // ═══════════════════════════════════════════════════════════

    @Override
    @CircuitBreaker(name = "policyTypeService", fallbackMethod = "payPremiumFallback")
    @Transactional
    public PremiumResponse payPremium(Long customerId, PremiumPaymentRequest request) {

        Policy policy = getPolicy(request.getPolicyId());

        if (!policy.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You can only pay premiums for your own policies");
        }

        Premium premium = premiumRepository
                .findByIdAndPolicyId(request.getPremiumId(), request.getPolicyId())
                .orElseThrow(() -> new PremiumNotFoundException(request.getPremiumId(), request.getPolicyId()));

        if (premium.getStatus() == Premium.PremiumStatus.PAID) {
            throw new IllegalStateException("Premium is already paid");
        }
        if (premium.getStatus() == Premium.PremiumStatus.WAIVED) {
            throw new IllegalStateException("Waived premiums cannot be paid");
        }

        premium.setStatus(Premium.PremiumStatus.PAID);
        premium.setPaidDate(LocalDate.now());
        premium.setPaymentMethod(request.getPaymentMethod());
        premium.setPaymentReference(request.getPaymentReference() != null
                ? request.getPaymentReference()
                : "TXN-" + UUID.randomUUID().toString().substring(0, 8));

        Premium saved = premiumRepository.save(premium);
        saveAudit(policy.getId(), customerId, "CUSTOMER", "PREMIUM_PAID",
                Premium.PremiumStatus.PENDING.name(), Premium.PremiumStatus.PAID.name(),
                "Premium ID: " + premium.getId() + ", Ref: " + premium.getPaymentReference());
        CustomerProfileResponse profile = getCustomerProfileSafely(customerId);

        notificationPublisher.publishPremiumPaid(
                PremiumPaidEvent.builder()
                        .premiumId(saved.getId())
                        .policyId(policy.getId())
                        .policyNumber(policy.getPolicyNumber())
                        .customerId(customerId)
                        .customerEmail(profile.getEmail())
                        .customerName(profile.getName())
                        .amount(saved.getAmount())
                        .paidDate(saved.getPaidDate())
                        .paymentMethod(saved.getPaymentMethod() != null
                                ? saved.getPaymentMethod().name() : null)
                        .paymentReference(saved.getPaymentReference())
                        .build());

        return mapPremium(saved);
    }

    public PremiumResponse payPremiumFallback(Long customerId, PremiumPaymentRequest request, Throwable t) {
        log.error("payPremium CIRCUIT BREAKER fallback — customerId={}, reason={}", customerId, t.getMessage());
        throw new ServiceUnavailableException("Premium payment service", t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PremiumResponse> getPremiumsByPolicy(Long policyId) {
        return premiumRepository.findByPolicyId(policyId)
                .stream()
                .map(this::mapPremium)
                .toList();
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = com.smartSure.PolicyService.config.RabbitMQConfig.QUEUE_PAYMENT_COMPLETED)
    @Transactional
    public void handlePaymentCompleted(com.smartSure.PolicyService.dto.event.PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for premiumId={}", event.getPremiumId());
        try {
            Premium premium = premiumRepository.findById(event.getPremiumId())
                    .orElseThrow(() -> new PremiumNotFoundException(event.getPremiumId(), event.getPolicyId()));
            
            if (premium.getStatus() == Premium.PremiumStatus.PAID) {
                log.info("Premium {} is already paid, ignoring event", premium.getId());
                return;
            }
            
            premium.setStatus(Premium.PremiumStatus.PAID);
            premium.setPaidDate(event.getPaidAt() != null ? event.getPaidAt().toLocalDate() : LocalDate.now());
            
            if (event.getPaymentMethod() != null) {
                try {
                    premium.setPaymentMethod(Premium.PaymentMethod.valueOf(event.getPaymentMethod()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown payment method: {}", event.getPaymentMethod());
                }
            }
            
            premium.setPaymentReference(event.getRazorpayPaymentId());
            premiumRepository.save(premium);

            // Improvement: Activate the policy if this is the first payment and start date is valid
            if (premium.getPolicy().getStatus() == Policy.PolicyStatus.CREATED) {
                if (!premium.getPolicy().getStartDate().isAfter(LocalDate.now())) {
                    premium.getPolicy().setStatus(Policy.PolicyStatus.ACTIVE);
                    policyRepository.save(premium.getPolicy());
                    saveAudit(premium.getPolicy().getId(), event.getCustomerId(), "SYSTEM", "ACTIVATED",
                            Policy.PolicyStatus.CREATED.name(), Policy.PolicyStatus.ACTIVE.name(),
                            "Policy activated upon first premium payment");
                }
            }

            saveAudit(premium.getPolicy().getId(), event.getCustomerId(), "SYSTEM", "PREMIUM_PAID",
                    Premium.PremiumStatus.PENDING.name(), Premium.PremiumStatus.PAID.name(),
                    "Premium paid via payment tracking");

            CustomerProfileResponse profile = getCustomerProfileSafely(premium.getPolicy().getCustomerId());
            notificationPublisher.publishPremiumPaid(
                    PremiumPaidEvent.builder()
                            .premiumId(premium.getId())
                            .policyId(premium.getPolicy().getId())
                            .policyNumber(premium.getPolicy().getPolicyNumber())
                            .customerId(event.getCustomerId())
                            .customerEmail(profile.getEmail())
                            .customerName(profile.getName())
                            .amount(premium.getAmount())
                            .paidDate(premium.getPaidDate())
                            .paymentMethod(premium.getPaymentMethod() != null ? premium.getPaymentMethod().name() : null)
                            .paymentReference(premium.getPaymentReference())
                            .build()
            );

        } catch (Exception e) {
            log.error("Failed to process payment completed event for premiumId={}: {}", event.getPremiumId(), e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public PolicyResponse adminUpdatePolicyStatus(Long policyId, PolicyStatusUpdateRequest request) {

        Policy policy = getPolicy(policyId);
        String prevStatus = policy.getStatus().name();

        policy.setStatus(request.getStatus());
        if (request.getReason() != null) {
            policy.setCancellationReason(request.getReason());
        }

        Policy saved = policyRepository.save(policy);
        saveAudit(policyId, 0L, "ADMIN", "STATUS_CHANGED",
                prevStatus, request.getStatus().name(), request.getReason());

        return policyMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicySummaryResponse getPolicySummary() {
        return PolicySummaryResponse.builder()
                .totalPolicies(policyRepository.count())
                .activePolicies(policyRepository.countByStatus(Policy.PolicyStatus.ACTIVE))
                .expiredPolicies(policyRepository.countByStatus(Policy.PolicyStatus.EXPIRED))
                .cancelledPolicies(policyRepository.countByStatus(Policy.PolicyStatus.CANCELLED))
                .totalPremiumCollected(premiumRepository.totalPremiumCollected(Premium.PremiumStatus.PAID))
                .totalCoverageProvided(policyRepository.sumActiveCoverages())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // PREMIUM CALCULATION
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PremiumCalculationResponse calculatePremium(PremiumCalculationRequest request) {
        PolicyType type = policyTypeRepository.findById(request.getPolicyTypeId())
                .orElseThrow(() -> new PolicyTypeNotFoundException(request.getPolicyTypeId()));
        return premiumCalculator.calculatePremium(
                type, request.getCoverageAmount(),
                request.getPaymentFrequency(), request.getCustomerAge());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateCoverage(Long policyId, Long userId, boolean isAdmin) {
        Policy policy = getPolicy(policyId);
        if (!isAdmin && !policy.getCustomerId().equals(userId)) {
            return false;
        }
        return policy.getStatus() == Policy.PolicyStatus.ACTIVE;
    }

    // ═══════════════════════════════════════════════════════════
    // SCHEDULERS
    // ═══════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expirePolicies() {
        List<Policy> expired = policyRepository.findExpiredActivePolicies(
                Policy.PolicyStatus.ACTIVE, LocalDate.now());
        expired.forEach(p -> {
            p.setStatus(Policy.PolicyStatus.EXPIRED);
            saveAudit(p.getId(), 0L, "SYSTEM", "EXPIRED",
                    Policy.PolicyStatus.ACTIVE.name(),
                    Policy.PolicyStatus.EXPIRED.name(),
                    "Auto-expired by scheduler");
        });
        log.info("Expiry scheduler: {} policies expired", expired.size());
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void markOverduePremiums() {
        List<Premium> overdue = premiumRepository.findOverduePremiums(
                Premium.PremiumStatus.PENDING, LocalDate.now());
        overdue.forEach(p -> p.setStatus(Premium.PremiumStatus.OVERDUE));
        log.info("Overdue scheduler: {} premiums marked overdue", overdue.size());
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendPremiumDueReminders() {
        LocalDate reminderDate = LocalDate.now().plusDays(7);
        premiumRepository.findByStatus(Premium.PremiumStatus.PENDING)
                .stream()
                .filter(p -> p.getDueDate().equals(reminderDate))
                .forEach(p -> {
                    CustomerProfileResponse profile = getCustomerProfileSafely(p.getPolicy().getCustomerId());
                    notificationPublisher.publishPremiumDueReminder(
                            PremiumDueReminderEvent.builder()
                                    .premiumId(p.getId())
                                    .policyId(p.getPolicy().getId())
                                    .policyNumber(p.getPolicy().getPolicyNumber())
                                    .customerId(p.getPolicy().getCustomerId())
                                    .customerEmail(profile.getEmail())
                                    .customerName(profile.getName())
                                    .amount(p.getAmount())
                                    .dueDate(p.getDueDate())
                                    .build());
                });
        log.info("Premium reminder scheduler fired for due date: {}", reminderDate);
    }

    @Scheduled(cron = "0 5 9 * * *")
    @Transactional(readOnly = true)
    public void sendExpiryReminders() {
        LocalDate reminderDate = LocalDate.now().plusDays(30);
        policyRepository.findExpiringPolicies(
                        Policy.PolicyStatus.ACTIVE, reminderDate, reminderDate)
                .forEach(p -> {
                    CustomerProfileResponse profile = getCustomerProfileSafely(p.getCustomerId());
                    notificationPublisher.publishPolicyExpiryReminder(
                            PolicyExpiryReminderEvent.builder()
                                    .policyId(p.getId())
                                    .policyNumber(p.getPolicyNumber())
                                    .customerId(p.getCustomerId())
                                    .customerEmail(profile.getEmail())
                                    .customerName(profile.getName())
                                    .policyTypeName(p.getPolicyType().getName())
                                    .endDate(p.getEndDate())
                                    .build());
                });
        log.info("Expiry reminder scheduler fired for date: {}", reminderDate);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private Policy getPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException(id));
    }

    private PolicyResponse buildDetailedResponse(Policy policy) {
        List<PremiumResponse> premiums = getPremiumsByPolicy(policy.getId());
        return policyMapper.toResponseWithPremiums(policy, premiums);
    }

    private PremiumResponse mapPremium(Premium premium) {
        return PremiumResponse.builder()
                .id(premium.getId())
                .amount(premium.getAmount())
                .dueDate(premium.getDueDate())
                .paidDate(premium.getPaidDate())
                .status(premium.getStatus().name())
                .paymentReference(premium.getPaymentReference())
                .paymentMethod(premium.getPaymentMethod() != null
                        ? premium.getPaymentMethod().name() : null)
                .build();
    }

    private void generatePremiumSchedule(Policy policy, int termMonths) {
        int interval  = premiumCalculator.monthsBetweenInstallments(policy.getPaymentFrequency());
        int count     = premiumCalculator.installmentCount(termMonths, policy.getPaymentFrequency());
        LocalDate dueDate = policy.getStartDate();
        List<Premium> premiums = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            premiums.add(Premium.builder()
                    .policy(policy)
                    .amount(policy.getPremiumAmount())
                    .dueDate(dueDate)
                    .status(Premium.PremiumStatus.PENDING)
                    .build());
            dueDate = dueDate.plusMonths(interval);
        }
        premiumRepository.saveAll(premiums);
    }

    private void saveAudit(Long policyId, Long actorId, String actorRole,
                           String action, String fromStatus, String toStatus, String details) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .policyId(policyId)
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .action(action)
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .details(details)
                    .build());
        } catch (Exception ex) {
            log.error("Audit log save failed for policyId={}: {}", policyId, ex.getMessage());
        }
    }

    private String generatePolicyNumber() {
        return "POL-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    private PolicyPageResponse toPageResponse(Page<Policy> page) {
        return PolicyPageResponse.builder()
                .content(page.getContent().stream().map(policyMapper::toResponse).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Cacheable(value = "customerEmail", key = "#customerId")
    private CustomerProfileResponse getCustomerProfileSafely(Long customerId) {
        try {
            return authServiceClient.getCustomerProfile(customerId);
        } catch (Exception e) {
            log.warn("Could not fetch profile for customerId={}: {}", customerId, e.getMessage());
            return CustomerProfileResponse.builder()
                    .id(customerId)
                    .name("Customer")
                    .email(null)
                    .build();
        }
    }
}