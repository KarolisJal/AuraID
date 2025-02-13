package com.aura.auraid.service;

import com.aura.auraid.model.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;

public interface AuditService {
    void logEvent(String action, String username, String entityType, String entityId, String details);
    void logEvent(String action, String username, String entityType, String entityId, String details, HttpServletRequest request);
    List<AuditLog> getRecentLogs(int limit);
    List<AuditLog> getRecentLogsByAction(String action, int limit);
    List<AuditLog> getUserActivity(String username, int limit);
    Map<String, Long> getActivityStatistics(LocalDateTime since);
    List<AuditLog> getFailedLoginAttempts(int limit);
    List<AuditLog> getSuspiciousActivities(LocalDateTime since);
    Map<String, Integer> getRecentFailedLoginAttempts(LocalDateTime since);
    
    // New methods for enhanced audit functionality
    List<AuditLog> searchAuditLogs(String username, String action, String entityType, 
                                  String ipAddress, LocalDateTime startDate, 
                                  LocalDateTime endDate, int limit);
    
    Map<String, Object> getUserActivitySummary(String username, LocalDateTime since);
    
    List<AuditLog> getAuditLogsForExport(LocalDateTime startDate, LocalDateTime endDate, 
                                        String username, String action);
    
    List<Map<String, Object>> getIpAddressActivity(LocalDateTime since, int limit);
    
    Map<String, Map<String, Long>> getActionTrends(LocalDateTime since);
    
    // New methods for dashboard functionality
    List<AuditLog> getRecentLogsByPeriod(LocalDateTime since);
    Map<String, Long> getIpAddressDistribution(LocalDateTime since);
} 