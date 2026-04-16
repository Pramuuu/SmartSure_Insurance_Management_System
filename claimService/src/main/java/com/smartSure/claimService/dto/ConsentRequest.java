package com.smartSure.claimService.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for digital consent.
 * Customer checks "I confirm all information is accurate" in the UI.
 * Backend records timestamp + IP address as legal signature (IT Act 2000).
 */
@Getter
@Setter
@NoArgsConstructor
public class ConsentRequest {
    // Frontend sends true when customer checks the consent checkbox
    private boolean consentGiven;
}
