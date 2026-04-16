package com.smartSure.adminService.service;

import com.smartSure.adminService.dto.AuditLogDTO;
import com.smartSure.adminService.entity.AuditLog;
import com.smartSure.adminService.exception.ResourceNotFoundException;
import com.smartSure.adminService.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog log(Long adminId, String action, String targetEntity,
                        Long targetId, String remarks) {
        AuditLog log = new AuditLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setRemarks(remarks);
        return auditLogRepository.save(log);
    }

    public List<AuditLogDTO> getAllLogs() {
        return auditLogRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByAdmin(Long adminId) {
        return auditLogRepository.findByAdminId(adminId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByEntity(String targetEntity) {
        return auditLogRepository.findByTargetEntity(targetEntity).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByEntityAndId(String targetEntity, Long targetId) {
        return auditLogRepository.findByTargetEntityAndTargetId(targetEntity, targetId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByDateRange(LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByDateRange(from, to).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getRecentLogs(int limit) {
        return auditLogRepository.findRecentLogs(PageRequest.of(0, limit)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AuditLogDTO getLogById(Long id) {
        return toDTO(auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AuditLog not found with id: " + id)));
    }

    private AuditLogDTO toDTO(AuditLog log) {
        return AuditLogDTO.builder()
                .id(log.getId())
                .adminId(log.getAdminId())
                .action(log.getAction())
                .targetEntity(log.getTargetEntity())
                .targetId(log.getTargetId())
                .remarks(log.getRemarks())
                .performedAt(log.getPerformedAt())
                .build();
    }
}