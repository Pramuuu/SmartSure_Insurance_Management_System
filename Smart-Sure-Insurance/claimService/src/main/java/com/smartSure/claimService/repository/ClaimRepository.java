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

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.amount), 0) FROM Claim c WHERE c.policyId = :policyId AND c.status = 'APPROVED'")
    java.math.BigDecimal sumApprovedClaimsByPolicyId(@org.springframework.data.repository.query.Param("policyId") Long policyId);

    List<Claim> findByStatusIn(List<Status> statuses);


    /** ADDED: Check if a claim exists for a user and policy */
    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);


    //  — checks for open claim excluding terminal statuses
    boolean existsByUserIdAndPolicyIdAndStatusNotIn(
            Long userId,
            Long policyId,
            List<Status> statuses
    );
}
