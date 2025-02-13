package com.aura.auraid.service.impl;

import com.aura.auraid.model.AuditLog;
import com.aura.auraid.repository.AuditLogRepository;
import com.aura.auraid.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void logEvent(String action, String username, String entityType, String entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .username(username)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    public void logEvent(String action, String username, String entityType, String entityId, String details, HttpServletRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .username(username)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Override
    public List<AuditLog> getRecentLogsByAction(String action, int limit) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, 
            PageRequest.of(0, limit));
    }

    @Override
    public List<AuditLog> getUserActivity(String username, int limit) {
        return auditLogRepository.findByUsernameOrderByCreatedAtDesc(username, 
            PageRequest.of(0, limit));
    }

    @Override
    public Map<String, Long> getActivityStatistics(LocalDateTime since) {
        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(since, LocalDateTime.now());
        
        return logs.stream()
            .collect(Collectors.groupingBy(
                AuditLog::getAction,
                Collectors.counting()
            ));
    }

    @Override
    public List<AuditLog> getFailedLoginAttempts(int limit) {
        return auditLogRepository.findByActionAndDetailsContainingOrderByCreatedAtDesc(
            "LOGIN", 
            "failed", 
            PageRequest.of(0, limit));
    }

    @Override
    public List<AuditLog> getSuspiciousActivities(LocalDateTime since) {
        List<AuditLog> allActivities = auditLogRepository.findByCreatedAtBetween(since, LocalDateTime.now());
        
        // Group activities by IP address
        Map<String, List<AuditLog>> activitiesByIp = allActivities.stream()
            .collect(Collectors.groupingBy(AuditLog::getIpAddress));
        
        // Identify suspicious IPs (those with multiple failed attempts or unusual patterns)
        return activitiesByIp.entrySet().stream()
            .filter(entry -> isSuspiciousActivity(entry.getValue()))
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getRecentFailedLoginAttempts(LocalDateTime since) {
        List<AuditLog> failedLogins = auditLogRepository.findByActionAndDetailsContainingAndCreatedAtBetween(
            "LOGIN", "failed", since, LocalDateTime.now());
        
        return failedLogins.stream()
            .collect(Collectors.groupingBy(
                AuditLog::getUsername,
                Collectors.collectingAndThen(
                    Collectors.counting(),
                    Long::intValue
                )
            ));
    }

    @Override
    public List<AuditLog> searchAuditLogs(String username, String action, String entityType,
                                         String ipAddress, LocalDateTime startDate,
                                         LocalDateTime endDate, int limit) {
        // If no dates provided, use last 24 hours as default
        LocalDateTime effectiveStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(1);
        LocalDateTime effectiveEndDate = endDate != null ? endDate : LocalDateTime.now();

        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(effectiveStartDate, effectiveEndDate);

        return logs.stream()
            .filter(log -> username == null || log.getUsername().equalsIgnoreCase(username))
            .filter(log -> action == null || log.getAction().equals(action))
            .filter(log -> entityType == null || log.getEntityType().equals(entityType))
            .filter(log -> ipAddress == null || log.getIpAddress().equals(ipAddress))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserActivitySummary(String username, LocalDateTime since) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusDays(30);
        List<AuditLog> userLogs = auditLogRepository.findByUsernameAndCreatedAtBetween(
            username, effectiveSince, LocalDateTime.now());

        Map<String, Object> summary = new HashMap<>();
        
        // Most frequent actions
        Map<String, Long> actionCounts = userLogs.stream()
            .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.put("actionCounts", actionCounts);

        // IP addresses used
        Map<String, Long> ipAddressCounts = userLogs.stream()
            .filter(log -> log.getIpAddress() != null)
            .collect(Collectors.groupingBy(AuditLog::getIpAddress, Collectors.counting()));
        summary.put("ipAddresses", ipAddressCounts);

        // Success vs. failure ratio
        long totalActions = userLogs.size();
        long failedActions = userLogs.stream()
            .filter(log -> log.getDetails() != null && log.getDetails().contains("failed"))
            .count();
        summary.put("totalActions", totalActions);
        summary.put("failedActions", failedActions);
        summary.put("successRate", totalActions > 0 ? 
            ((double)(totalActions - failedActions) / totalActions) * 100 : 100);

        // Most recent activity
        summary.put("lastActivity", userLogs.stream()
            .max(Comparator.comparing(AuditLog::getCreatedAt))
            .map(AuditLog::getCreatedAt)
            .orElse(null));

        return summary;
    }

    @Override
    public List<AuditLog> getAuditLogsForExport(LocalDateTime startDate, LocalDateTime endDate,
                                               String username, String action) {
        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(startDate, endDate);

        return logs.stream()
            .filter(log -> username == null || log.getUsername().equalsIgnoreCase(username))
            .filter(log -> action == null || log.getAction().equals(action))
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getIpAddressActivity(LocalDateTime since, int limit) {
        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(since, LocalDateTime.now());

        return logs.stream()
            .filter(log -> log.getIpAddress() != null)
            .collect(Collectors.groupingBy(AuditLog::getIpAddress))
            .entrySet().stream()
            .map(entry -> {
                Map<String, Object> activity = new HashMap<>();
                List<AuditLog> ipLogs = entry.getValue();
                
                activity.put("ipAddress", entry.getKey());
                activity.put("totalActions", ipLogs.size());
                activity.put("uniqueUsers", ipLogs.stream()
                    .map(AuditLog::getUsername)
                    .distinct()
                    .count());
                activity.put("lastActivity", ipLogs.stream()
                    .max(Comparator.comparing(AuditLog::getCreatedAt))
                    .map(AuditLog::getCreatedAt)
                    .orElse(null));
                activity.put("failedAttempts", ipLogs.stream()
                    .filter(log -> log.getDetails() != null && 
                                 log.getDetails().contains("failed"))
                    .count());
                
                return activity;
            })
            .sorted(Comparator.comparingInt(m -> -((Integer) m.get("totalActions"))))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Map<String, Long>> getActionTrends(LocalDateTime since) {
        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetween(since, LocalDateTime.now());
        Map<String, Map<String, Long>> trends = new HashMap<>();

        // Group by time periods
        Map<String, List<AuditLog>> hourly = groupByTimePeriod(logs, since, ChronoUnit.HOURS);
        Map<String, List<AuditLog>> daily = groupByTimePeriod(logs, since, ChronoUnit.DAYS);
        Map<String, List<AuditLog>> weekly = groupByTimePeriod(logs, since, ChronoUnit.WEEKS);

        // Calculate trends for each action
        trends.put("hourly", calculateActionCountsForPeriod(hourly));
        trends.put("daily", calculateActionCountsForPeriod(daily));
        trends.put("weekly", calculateActionCountsForPeriod(weekly));

        return trends;
    }

    private Map<String, List<AuditLog>> groupByTimePeriod(List<AuditLog> logs, 
                                                         LocalDateTime since,
                                                         ChronoUnit unit) {
        return logs.stream()
            .collect(Collectors.groupingBy(log -> {
                long periodsBetween = unit.between(since, log.getCreatedAt());
                return String.format("%d %s ago", periodsBetween, unit.name().toLowerCase());
            }));
    }

    private Map<String, Long> calculateActionCountsForPeriod(Map<String, List<AuditLog>> periodLogs) {
        return periodLogs.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream()
                    .map(AuditLog::getAction)
                    .count()
            ));
    }

    @Override
    public List<AuditLog> getRecentLogsByPeriod(LocalDateTime since) {
        return auditLogRepository.findByCreatedAtBetween(since, LocalDateTime.now());
    }

    @Override
    public Map<String, Long> getIpAddressDistribution(LocalDateTime since) {
        List<AuditLog> logs = getRecentLogsByPeriod(since);
        
        return logs.stream()
            .filter(log -> log.getIpAddress() != null)
            .collect(Collectors.groupingBy(
                AuditLog::getIpAddress,
                Collectors.counting()
            ));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    private boolean isSuspiciousActivity(List<AuditLog> activities) {
        long failedAttempts = activities.stream()
            .filter(log -> log.getAction().equals("LOGIN") && 
                          log.getDetails().contains("failed"))
            .count();
        
        // Consider it suspicious if there are more than 5 failed attempts
        return failedAttempts >= 5;
    }
} 