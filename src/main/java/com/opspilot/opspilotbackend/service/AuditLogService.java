package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.entity.AuditLog;

import java.util.List;

public interface AuditLogService {

    AuditLog createAuditLog(
            Long userId,
            String action,
            String entityType,
            Long entityId,
            String details
    );

    List<AuditLog> getAllAuditLogs();
}