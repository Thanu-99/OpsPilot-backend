package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.entity.AuditLog;
import com.opspilot.opspilotbackend.repository.AuditLogRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog createAuditLog(
            Long userId,
            String action,
            String entityType,
            Long entityId,
            String details) {

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        return auditLogRepository.save(auditLog);
    }

    @Override
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }
}