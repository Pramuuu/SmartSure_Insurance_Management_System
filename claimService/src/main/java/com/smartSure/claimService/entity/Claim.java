package com.smartSure.claimService.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Claim entity representing an insurance claim raised against a policy.
 * ADDED: userId for ownership validation, description, incidentDate.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long policyId;

    /**
     * ADDED: The user who owns this claim (from X-User-Id header).
     * Used for ownership validation — customers can only see/modify their own claims.
     */
    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate incidentDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fileName", column = @Column(name = "claim_form_name")),
        @AttributeOverride(name = "fileType", column = @Column(name = "claim_form_type")),
        @AttributeOverride(name = "data", column = @Column(name = "claim_form_data", columnDefinition = "LONGBLOB"))
    })
    private FileData claimForm;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fileName", column = @Column(name = "evidence_name")),
        @AttributeOverride(name = "fileType", column = @Column(name = "evidence_type")),
        @AttributeOverride(name = "data", column = @Column(name = "evidence_data", columnDefinition = "LONGBLOB"))
    })
    private FileData evidences;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fileName", column = @Column(name = "aadhaar_name")),
        @AttributeOverride(name = "fileType", column = @Column(name = "aadhaar_type")),
        @AttributeOverride(name = "data", column = @Column(name = "aadhaar_data", columnDefinition = "LONGBLOB"))
    })
    private FileData aadhaarCard;

    private BigDecimal amount;
    private LocalDateTime timeOfCreation;

    @PrePersist
    public void prePersist() {
        this.timeOfCreation = LocalDateTime.now();
        if (this.status == null) {
            this.status = Status.DRAFT;
        }
    }
}
