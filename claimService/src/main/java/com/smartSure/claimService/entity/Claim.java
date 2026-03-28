package com.smartSure.claimService.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long policyId;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fileName", column = @Column(name = "claim_form_name")),
        @AttributeOverride(name = "fileType", column = @Column(name = "claim_form_type")),
        @AttributeOverride(name = "data", column = @Column(name = "claim_form_data"))
    })
    private FileData claimForm;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fileName", column = @Column(name = "evidence_name")),
        @AttributeOverride(name = "fileType", column = @Column(name = "evidence_type")),
        @AttributeOverride(name = "data", column = @Column(name = "evidence_data"))
    })
    private FileData evidences;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fileName", column = @Column(name = "aadhaar_name")),
        @AttributeOverride(name = "fileType", column = @Column(name = "aadhaar_type")),
        @AttributeOverride(name = "data", column = @Column(name = "aadhaar_data"))
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
