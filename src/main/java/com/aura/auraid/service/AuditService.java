package com.aura.auraid.service;

import com.aura.auraid.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface AuditService {
    void logEvent(String action, String username, String entityType, String entityId, String details);
    void logEvent(String action, String username, String entityType, String entityId, String details, HttpServletRequest request);
    List<AuditLog> getRecentLogs(int limit);
} 