package com.smartSure.PolicyService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePolicyException extends RuntimeException {

    public DuplicatePolicyException() {
        super("You already have an active policy of this type. You can purchase this plan again once your current policy expires or its coverage is fully utilized.");
    }
}