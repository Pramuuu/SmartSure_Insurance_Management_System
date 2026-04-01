package com.smartSure.claimService.repository;

import com.smartSure.claimService.entity.Claim;
import com.smartSure.claimService.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Claim entity.
 * ADDED: findByUserId, findByPolicyId for ownership filtering.
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByStatus(Status status);

    /** ADDED: Customer lists their own claims */
    List<Claim> findByUserId(Long userId);

    /** ADDED: Find all claims for a policy */
    List<Claim> findByPolicyId(Long policyId);

    /** ADDED: Check if a claim exists for a user and policy */
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);
}
