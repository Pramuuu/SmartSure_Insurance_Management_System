package com.smartSure.claimService.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String current, String next) {
        super("Invalid status transition from " + current + " to " + next);
    }
}
