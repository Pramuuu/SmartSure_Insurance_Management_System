package com.smartSure.adminService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String targetEntity;

    @Column(nullable = false)
    private Long targetId;

    private String remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime performedAt = LocalDateTime.now();
}
