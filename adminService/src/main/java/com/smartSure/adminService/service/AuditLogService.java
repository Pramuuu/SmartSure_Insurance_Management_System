package com.smartSure.adminService.service;

import com.smartSure.adminService.entity.AuditLog;
import com.smartSure.adminService.exception.ResourceNotFoundException;
import com.smartSure.adminService.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // Log an admin action — called after every approve/reject/update
    public AuditLog log(Long adminId, String action, String targetEntity, Long targetId, String remarks) {
        AuditLog log = new AuditLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setRemarks(remarks);
        return auditLogRepository.save(log);
    }

    // Get all logs by admin
    public List<AuditLog> getLogsByAdmin(Long adminId) {
        return auditLogRepository.findByAdminId(adminId);
    }

    // Get all logs — for full audit trail view
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    // Get all logs for a specific entity type
    public List<AuditLog> getLogsByEntity(String targetEntity) {
        return auditLogRepository.findByTargetEntity(targetEntity);
    }

    // Get full history of a specific record
    public List<AuditLog> getLogsByEntityAndId(String targetEntity, Long targetId) {
        return auditLogRepository.findByTargetEntityAndTargetId(targetEntity, targetId);
    }

    // Get logs within a date range — for generating reports
    public List<AuditLog> getLogsByDateRange(LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByDateRange(from, to);
    }

    // Get most recent N logs — for admin dashboard activity feed
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findRecentLogs(PageRequest.of(0, limit));
    }

    // Get a single log by ID
    public AuditLog getLogById(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog not found with id: " + id));
    }
}
