package com.smartSure.adminService.repository;

import com.smartSure.adminService.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Get all logs by a specific admin
    List<AuditLog> findByAdminId(Long adminId);

    // Get all logs for a specific entity type (e.g. "Claim", "Policy")
    List<AuditLog> findByTargetEntity(String targetEntity);

    // Get all logs for a specific record (e.g. all actions on Claim #5)
    List<AuditLog> findByTargetEntityAndTargetId(String targetEntity, Long targetId);

    // Get logs within a date range — for audit reports
    @Query("SELECT a FROM AuditLog a WHERE a.performedAt BETWEEN :from AND :to ORDER BY a.performedAt DESC")
    List<AuditLog> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Get recent logs — for admin dashboard activity feed
    @Query("SELECT a FROM AuditLog a ORDER BY a.performedAt DESC")
    List<AuditLog> findRecentLogs(org.springframework.data.domain.Pageable pageable);
}
