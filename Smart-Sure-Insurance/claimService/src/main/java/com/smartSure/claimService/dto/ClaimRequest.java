package com.smartSure.claimService.dto;

import com.smartSure.claimService.entity.Claim;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for creating a digital claim.
 *
 * Customer fills this form in the Angular UI — no PDF download needed.
 * Backend auto-generates the PDF from this data.
 *
 * Required fields for all claims:
 *   policyId, incidentType, incidentDate, description, claimAmount
 *
 * Conditional fields (filled based on incidentType):
 *   HEALTH     → hospitalName, treatmentType
 *   AUTO       → vehicleNumber, garageRepairShop
 *   THEFT/ACC  → policeReportNumber, witnessName, witnessContact
 */
@Getter
@Setter
@NoArgsConstructor
public class ClaimRequest {

    // ── Required for all claims ───────────────────────────────────────────────

    @NotNull(message = "Policy ID is required")
    private Long policyId;

    @NotNull(message = "Incident type is required")
    private Claim.IncidentType incidentType;

    @NotNull(message = "Incident date is required")
    private String incidentDate;              // ISO date — "2024-03-25"

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Claim amount is required")
    @DecimalMin(value = "1.00", message = "Claim amount must be at least ₹1")
    private BigDecimal claimAmount;

    @NotBlank(message = "Incident location is required")
    private String incidentLocation;

    // ── Health claim fields ───────────────────────────────────────────────────
    private String hospitalName;
    private String treatmentType;             // INPATIENT, OUTPATIENT, EMERGENCY

    // ── Auto claim fields ─────────────────────────────────────────────────────
    private String vehicleNumber;
    private String garageRepairShop;

    // ── Theft / Accident fields ───────────────────────────────────────────────
    private String policeReportNumber;
    private String witnessName;
    private String witnessContact;
}