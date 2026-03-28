package com.smartSure.claimService.exception;

public class ClaimDeletionNotAllowedException extends RuntimeException {
    public ClaimDeletionNotAllowedException(Long claimId) {
        super("Claim " + claimId + " cannot be deleted because it is not in DRAFT status.");
    }
}
