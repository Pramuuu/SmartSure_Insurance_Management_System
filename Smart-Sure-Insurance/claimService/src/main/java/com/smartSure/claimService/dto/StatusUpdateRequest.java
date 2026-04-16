package com.smartSure.claimService.dto;

import com.smartSure.claimService.entity.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for admin status update.
 * remarks is mandatory when rejecting (IRDAI compliance).
 */
@Getter
@Setter
@NoArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private Status nextStatus;

    // Mandatory when nextStatus = REJECTED
    private String remarks;
}