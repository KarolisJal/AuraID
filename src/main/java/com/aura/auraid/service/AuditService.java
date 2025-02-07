package com.aura.auraid.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditService {
    void logEvent(String action, String username, String entityType, String entityId, String details);
    void logEvent(String action, String username, String entityType, String entityId, String details, HttpServletRequest request);
} 