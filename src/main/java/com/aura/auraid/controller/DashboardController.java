package com.aura.auraid.controller;

import com.aura.auraid.dto.DashboardStatsDTO;
import com.aura.auraid.dto.SecurityMetricsDTO;
import com.aura.auraid.dto.UserActivityDTO;
import com.aura.auraid.dto.SecurityEventDTO;
import com.aura.auraid.model.AuditLog;
import com.aura.auraid.service.AuditService;
import com.aura.auraid.service.UserService;
import com.aura.auraid.metrics.CustomMetrics;
import com.aura.auraid.enums.SecurityEventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ClassLoadingMXBean;
import com.aura.auraid.service.SystemMetricsService;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Dashboard", description = "Admin dashboard endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class DashboardController {

    private final UserService userService;
    private final AuditService auditService;
    private final CustomMetrics customMetrics;
    private final SystemMetricsService systemMetricsService;
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve comprehensive dashboard statistics")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
            .totalUsers(userService.getTotalUsers())
            .activeUsers(userService.getActiveUsers())
            .securityMetrics(buildSecurityMetrics())
            .userMetrics(getUserMetrics())
            .systemHealth(getSystemHealth())
            .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/security-metrics")
    @Operation(summary = "Get security metrics", description = "Retrieve detailed security-related metrics")
    public ResponseEntity<SecurityMetricsDTO> getSecurityMetricsEndpoint() {
        return ResponseEntity.ok(buildSecurityMetrics());
    }

    @GetMapping("/user-trends")
    @Operation(summary = "Get user registration trends", 
              description = "Retrieve user registration statistics over different time periods")
    public ResponseEntity<Map<String, Object>> getUserTrends() {
        Map<String, Object> trends = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        trends.put("last24Hours", userService.getNewUsersCount(now.minusDays(1)));
        trends.put("lastWeek", userService.getNewUsersCount(now.minusWeeks(1)));
        trends.put("lastMonth", userService.getNewUsersCount(now.minusMonths(1)));
        trends.put("last3Months", userService.getNewUsersCount(now.minusMonths(3)));
        trends.put("lastYear", userService.getNewUsersCount(now.minusYears(1)));
        
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/user-activity/{username}")
    @Operation(summary = "Get user activity", description = "Retrieve activity logs for a specific user")
    public ResponseEntity<List<UserActivityDTO>> getUserActivity(
            @PathVariable String username,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<UserActivityDTO> activities = auditService.getUserActivity(username, limit)
            .stream()
            .map(log -> UserActivityDTO.builder()
                .username(log.getUsername())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .timestamp(log.getCreatedAt())
                .details(log.getDetails())
                .status(log.getDetails().contains("failed") ? "FAILED" : "SUCCESS")
                .build())
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/security-events")
    @Operation(summary = "Get security events", 
              description = "Retrieve security-related events and alerts")
    public ResponseEntity<Map<String, Object>> getSecurityEvents(
            @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours,
            @RequestParam(required = false) @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$") String severity) {
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        Map<String, Object> securityEvents = new HashMap<>();
        
        // Get failed login attempts and suspicious activities from audit service
        List<SecurityEventDTO> events = new ArrayList<>();
        
        // Process failed login attempts
        Map<String, Integer> failedLoginsByUser = auditService.getRecentFailedLoginAttempts(since);
        
        failedLoginsByUser.forEach((username, count) -> {
            String severityLevel = count > 5 ? "HIGH" : count > 3 ? "MEDIUM" : "LOW";
            if (severity == null || severity.equals(severityLevel)) {
                events.add(SecurityEventDTO.builder()
                    .eventType(SecurityEventType.MULTIPLE_FAILED_LOGINS.name())
                    .username(username)
                    .severity(severityLevel)
                    .description(count + " failed login attempts detected")
                    .timestamp(LocalDateTime.now())
                    .resolved(false)
                    .build());
            }
        });
        
        // Add suspicious IP access events
        auditService.getSuspiciousActivities(since).forEach(log -> {
            if (severity == null || severity.equals("HIGH")) {
                events.add(SecurityEventDTO.builder()
                    .eventType(SecurityEventType.SUSPICIOUS_IP_ACCESS.name())
                    .username(log.getUsername())
                    .ipAddress(log.getIpAddress())
                    .userAgent(log.getUserAgent())
                    .severity("HIGH")
                    .description("Suspicious activity detected from unusual IP")
                    .timestamp(log.getCreatedAt())
                    .resolved(false)
                    .build());
            }
        });
        
        securityEvents.put("events", events);
        securityEvents.put("totalEvents", events.size());
        securityEvents.put("period", hours + " hours");
        securityEvents.put("severityDistribution", getSeverityDistribution(events));
        
        return ResponseEntity.ok(securityEvents);
    }

    @GetMapping("/security-alerts")
    @Operation(summary = "Get active security alerts", 
              description = "Retrieve current security alerts that need attention")
    public ResponseEntity<List<SecurityEventDTO>> getActiveSecurityAlerts() {
        List<SecurityEventDTO> activeAlerts = new ArrayList<>();
        
        // Check for accounts with multiple failed login attempts
        auditService.getRecentFailedLoginAttempts(LocalDateTime.now().minusHours(1))
            .forEach((username, count) -> {
                if (count >= 5) {
                    activeAlerts.add(SecurityEventDTO.builder()
                        .eventType(SecurityEventType.ACCOUNT_LOCKED.name())
                        .username(username)
                        .severity("HIGH")
                        .description("Account temporarily locked due to multiple failed login attempts")
                        .timestamp(LocalDateTime.now())
                        .resolved(false)
                        .build());
                }
            });
        
        return ResponseEntity.ok(activeAlerts);
    }

    @GetMapping("/activity-heatmap")
    @Operation(summary = "Get activity heatmap data", 
              description = "Get activity distribution by hour and day of week")
    public ResponseEntity<Map<String, Object>> getActivityHeatmap(
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AuditLog> logs = auditService.getRecentLogsByPeriod(since);
        
        // Create 7x24 matrix for week heatmap
        int[][] heatmap = new int[7][24];
        Map<String, Integer> dayMapping = Map.of(
            "MONDAY", 0, "TUESDAY", 1, "WEDNESDAY", 2, "THURSDAY", 3,
            "FRIDAY", 4, "SATURDAY", 5, "SUNDAY", 6
        );
        
        logs.forEach(log -> {
            int dayOfWeek = dayMapping.get(log.getCreatedAt().getDayOfWeek().name());
            int hourOfDay = log.getCreatedAt().getHour();
            heatmap[dayOfWeek][hourOfDay]++;
        });
        
        Map<String, Object> result = new HashMap<>();
        result.put("heatmap", heatmap);
        result.put("maxValue", Arrays.stream(heatmap)
            .flatMapToInt(Arrays::stream)
            .max()
            .orElse(0));
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/geographic-distribution")
    @Operation(summary = "Get geographic activity distribution", 
              description = "Get activity distribution by country/region")
    public ResponseEntity<Map<String, Object>> getGeographicDistribution(
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("countries", userService.getCountryDistribution());
        distribution.put("ipAddresses", auditService.getIpAddressDistribution(since));
        
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/performance-metrics")
    @Operation(summary = "Get system performance metrics", 
              description = "Get detailed system performance metrics over time")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics(
            @RequestParam(defaultValue = "60") @Min(1) @Max(3600) int seconds,
            @RequestParam(defaultValue = "false") boolean highResolution) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get metrics based on resolution
        if (highResolution) {
            metrics.put("metrics", systemMetricsService.getHighResolutionMetrics(seconds));
        } else {
            metrics.put("metrics", systemMetricsService.getMetricsHistory(seconds / 60));
        }
        
        // Add summary statistics
        metrics.put("summary", systemMetricsService.getMetricsSummary());
        
        // Add JVM metrics
        metrics.put("jvm", getDetailedJvmMetrics());
        
        // Add response time metrics
        metrics.put("responseTime", getResponseTimeMetrics());
        
        // Add error rate metrics
        metrics.put("errorRates", getErrorRateMetrics());
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/user-behavior")
    @Operation(summary = "Get user behavior analytics", 
              description = "Get detailed user behavior patterns")
    public ResponseEntity<Map<String, Object>> getUserBehaviorAnalytics(
            @RequestParam(defaultValue = "30") @Min(1) @Max(90) int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<String, Object> behavior = new HashMap<>();
        
        // Session patterns
        behavior.put("averageSessionDuration", calculateAverageSessionDuration(since));
        behavior.put("peakUsageTimes", calculatePeakUsageTimes(since));
        behavior.put("commonUserPaths", getCommonUserPaths(since));
        behavior.put("userRetention", calculateUserRetention(since));
        
        return ResponseEntity.ok(behavior);
    }

    @GetMapping("/metrics/history")
    @Operation(summary = "Get historical metrics", 
              description = "Get system metrics history for the specified time period")
    public ResponseEntity<Map<String, Object>> getMetricsHistory(
            @RequestParam(defaultValue = "60") @Min(1) @Max(1440) int minutes) {
        return ResponseEntity.ok(systemMetricsService.getMetricsHistory(minutes));
    }

    private Map<String, Object> getUserMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalUsers", userService.getTotalUsers());
        metrics.put("activeUsers", userService.getActiveUsers());
        metrics.put("newUsersToday", userService.getNewUsersCount(LocalDateTime.now().minusDays(1)));
        metrics.put("newUsersThisWeek", userService.getNewUsersCount(LocalDateTime.now().minusWeeks(1)));
        metrics.put("newUsersThisMonth", userService.getNewUsersCount(LocalDateTime.now().minusMonths(1)));
        return metrics;
    }

    private Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Get latest metrics
        Map<String, Object> latestMetrics = systemMetricsService.getLatestMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Double> metrics = (Map<String, Double>) latestMetrics.getOrDefault("metrics", new HashMap<String, Double>());
        
        // Basic Status
        health.put("status", "UP");
        health.put("timestamp", latestMetrics.get("timestamp"));
        
        // Memory Metrics
        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", metrics.get("heapUsed"));
        memory.put("heapCommitted", metrics.get("heapCommitted"));
        memory.put("heapMax", metrics.get("heapMax"));
        memory.put("heapUtilization", metrics.get("heapUtilization"));
        memory.put("nonHeapUsed", metrics.get("nonHeapUsed"));
        memory.put("nonHeapCommitted", metrics.get("nonHeapCommitted"));
        health.put("memory", memory);
        
        // Thread Metrics
        Map<String, Object> threads = new HashMap<>();
        threads.put("threadCount", metrics.get("threadCount"));
        threads.put("peakThreadCount", metrics.get("peakThreadCount"));
        threads.put("daemonThreadCount", metrics.get("daemonThreadCount"));
        threads.put("totalStartedThreadCount", metrics.get("totalStartedThreadCount"));
        threads.put("deadlockedThreads", metrics.get("deadlockedThreads"));
        health.put("threads", threads);
        
        // System Metrics
        Map<String, Object> system = new HashMap<>();
        system.put("systemCpuLoad", metrics.get("systemCpuLoad"));
        system.put("processCpuLoad", metrics.get("processCpuLoad"));
        system.put("freePhysicalMemory", metrics.get("freePhysicalMemory"));
        system.put("totalPhysicalMemory", metrics.get("totalPhysicalMemory"));
        system.put("committedVirtualMemory", metrics.get("committedVirtualMemory"));
        system.put("processCpuTime", metrics.get("processCpuTime"));
        health.put("system", system);
        
        // Add historical summary with percentiles
        Map<String, Object> metricsSummary = systemMetricsService.getMetricsSummary();
        health.put("summary", metricsSummary);
        
        return health;
    }

    private SecurityMetricsDTO buildSecurityMetrics() {
        return SecurityMetricsDTO.builder()
            .securityScore(calculateSecurityScore())
            .loginAttempts((long) customMetrics.getLoginAttempts().count())
            .successfulLogins((long) customMetrics.getLoginSuccess().count())
            .failedLogins((long) customMetrics.getLoginFailure().count())
            .registrationAttempts((long) customMetrics.getRegistrationAttempts().count())
            .successfulRegistrations((long) customMetrics.getRegistrationSuccess().count())
            .failedRegistrations((long) customMetrics.getRegistrationFailure().count())
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    private int calculateSecurityScore() {
        int score = 100;
        Map<String, Integer> penalties = new HashMap<>();
        
        // Login-related metrics
        double failedLoginRate = customMetrics.getLoginFailure().count() / 
            Math.max(1.0, customMetrics.getLoginAttempts().count());
        
        if (failedLoginRate > 0.5) penalties.put("highFailedLoginRate", 25);
        else if (failedLoginRate > 0.3) penalties.put("moderateFailedLoginRate", 15);
        else if (failedLoginRate > 0.1) penalties.put("lowFailedLoginRate", 5);
        
        // Registration-related metrics
        double failedRegistrationRate = customMetrics.getRegistrationFailure().count() / 
            Math.max(1.0, customMetrics.getRegistrationAttempts().count());
        
        if (failedRegistrationRate > 0.5) penalties.put("highFailedRegistrationRate", 20);
        else if (failedRegistrationRate > 0.3) penalties.put("moderateFailedRegistrationRate", 10);
        else if (failedRegistrationRate > 0.1) penalties.put("lowFailedRegistrationRate", 5);
        
        // Suspicious activity metrics
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<AuditLog> recentSuspiciousActivities = auditService.getSuspiciousActivities(oneHourAgo);
        int suspiciousCount = recentSuspiciousActivities.size();
        
        if (suspiciousCount > 10) penalties.put("criticalSuspiciousActivity", 30);
        else if (suspiciousCount > 5) penalties.put("highSuspiciousActivity", 20);
        else if (suspiciousCount > 2) penalties.put("moderateSuspiciousActivity", 10);
        
        // Failed login attempts concentration
        Map<String, Integer> recentFailedLogins = auditService.getRecentFailedLoginAttempts(oneHourAgo);
        long usersWithMultipleFailures = recentFailedLogins.values().stream()
            .filter(count -> count >= 3)
            .count();
        
        if (usersWithMultipleFailures > 5) penalties.put("widespreadFailedLogins", 25);
        else if (usersWithMultipleFailures > 2) penalties.put("multipleFailedLogins", 15);
        
        // Calculate final score
        int totalPenalty = penalties.values().stream().mapToInt(Integer::intValue).sum();
        score = Math.max(0, score - totalPenalty);
        
        // Log security score components for monitoring
        log.info("Security Score Components - Base: 100, Penalties: {}, Final Score: {}", 
                penalties, score);
        
        return score;
    }

    private Map<String, Long> getSeverityDistribution(List<SecurityEventDTO> events) {
        return events.stream()
            .collect(Collectors.groupingBy(
                SecurityEventDTO::getSeverity,
                Collectors.counting()
            ));
    }

    private Map<String, Object> getDetailedJvmMetrics() {
        Map<String, Object> jvmMetrics = new HashMap<>();
        
        // Get latest metrics
        Map<String, Object> latestMetrics = systemMetricsService.getLatestMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Double> metrics = (Map<String, Double>) latestMetrics.getOrDefault("metrics", new HashMap<String, Double>());
        
        // Garbage Collection Metrics
        Map<String, Object> gcMetrics = new HashMap<>();
        metrics.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("gc_"))
            .forEach(entry -> gcMetrics.put(
                entry.getKey().substring(3), 
                entry.getValue()));
        jvmMetrics.put("gc", gcMetrics);
        
        // Memory Metrics
        Map<String, Object> memoryMetrics = new HashMap<>();
        memoryMetrics.put("heapUsed", metrics.get("heapUsed"));
        memoryMetrics.put("heapCommitted", metrics.get("heapCommitted"));
        memoryMetrics.put("heapMax", metrics.get("heapMax"));
        memoryMetrics.put("heapUtilization", metrics.get("heapUtilization"));
        memoryMetrics.put("nonHeapUsed", metrics.get("nonHeapUsed"));
        memoryMetrics.put("nonHeapCommitted", metrics.get("nonHeapCommitted"));
        jvmMetrics.put("memory", memoryMetrics);
        
        // Thread Metrics
        Map<String, Object> threadMetrics = new HashMap<>();
        threadMetrics.put("count", metrics.get("threadCount"));
        threadMetrics.put("peakCount", metrics.get("peakThreadCount"));
        threadMetrics.put("daemonCount", metrics.get("daemonThreadCount"));
        threadMetrics.put("totalStarted", metrics.get("totalStartedThreadCount"));
        threadMetrics.put("deadlocked", metrics.get("deadlockedThreads"));
        jvmMetrics.put("threads", threadMetrics);
        
        return jvmMetrics;
    }

    private Map<String, Object> getResponseTimeMetrics() {
        Map<String, Object> latestMetrics = systemMetricsService.getLatestMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Double> metrics = (Map<String, Double>) latestMetrics.getOrDefault("metrics", new HashMap<String, Double>());
        
        Map<String, Object> responseMetrics = new HashMap<>();
        responseMetrics.put("average", metrics.get("responseTime_avg"));
        responseMetrics.put("max", metrics.get("responseTime_max"));
        responseMetrics.put("min", metrics.get("responseTime_min"));
        
        // Add operation-specific metrics
        Map<String, Map<String, Object>> operationMetrics = new HashMap<>();
        metrics.entrySet().stream()
            .filter(entry -> entry.getKey().contains("_mean") || 
                           entry.getKey().contains("_max") || 
                           entry.getKey().contains("_count"))
            .forEach(entry -> {
                String[] parts = entry.getKey().split("_");
                String operation = parts[0];
                String metric = parts[1];
                
                operationMetrics.computeIfAbsent(operation, k -> new HashMap<>())
                    .put(metric, entry.getValue());
            });
        responseMetrics.put("operations", operationMetrics);
        
        return responseMetrics;
    }

    private Map<String, Object> getErrorRateMetrics() {
        Map<String, Object> errorMetrics = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Get error rates for different time periods
        errorMetrics.put("lastHour", calculateErrorRate(now.minusHours(1)));
        errorMetrics.put("today", calculateErrorRate(now.withHour(0)));
        errorMetrics.put("thisWeek", calculateErrorRate(now.minusWeeks(1)));
        
        return errorMetrics;
    }

    private double calculateErrorRate(LocalDateTime since) {
        List<AuditLog> logs = auditService.getRecentLogsByPeriod(since);
        long totalRequests = logs.size();
        long errorRequests = logs.stream()
            .filter(log -> log.getDetails() != null && 
                         (log.getDetails().contains("error") || 
                          log.getDetails().contains("failed")))
            .count();
        
        return totalRequests > 0 ? 
            (double) errorRequests / totalRequests * 100 : 0.0;
    }

    private Map<String, Object> calculateAverageSessionDuration(LocalDateTime since) {
        // Implementation would track user sessions
        // For now, returning placeholder data
        Map<String, Object> sessionMetrics = new HashMap<>();
        sessionMetrics.put("averageDuration", 1800); // seconds
        sessionMetrics.put("medianDuration", 1500); // seconds
        return sessionMetrics;
    }

    private List<Map<String, Object>> calculatePeakUsageTimes(LocalDateTime since) {
        List<AuditLog> logs = auditService.getRecentLogsByPeriod(since);
        
        return logs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getCreatedAt().getHour(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> {
                Map<String, Object> hourlyUsage = new HashMap<>();
                hourlyUsage.put("hour", entry.getKey());
                hourlyUsage.put("count", entry.getValue());
                return hourlyUsage;
            })
            .sorted((a, b) -> Long.compare(
                (Long) b.get("count"), 
                (Long) a.get("count")))
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getCommonUserPaths(LocalDateTime since) {
        List<AuditLog> logs = auditService.getRecentLogsByPeriod(since);
        
        // Group by user and analyze action sequences
        return logs.stream()
            .collect(Collectors.groupingBy(AuditLog::getUsername))
            .entrySet().stream()
            .map(entry -> {
                Map<String, Object> pathInfo = new HashMap<>();
                List<String> actions = entry.getValue().stream()
                    .map(AuditLog::getAction)
                    .collect(Collectors.toList());
                
                pathInfo.put("username", entry.getKey());
                pathInfo.put("actionSequence", actions);
                pathInfo.put("totalActions", actions.size());
                
                return pathInfo;
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> calculateUserRetention(LocalDateTime since) {
        Map<String, Object> retention = new HashMap<>();
        
        // Daily Active Users
        retention.put("dailyActiveUsers", calculateActiveUsers(1));
        
        // Weekly Active Users
        retention.put("weeklyActiveUsers", calculateActiveUsers(7));
        
        // Monthly Active Users
        retention.put("monthlyActiveUsers", calculateActiveUsers(30));
        
        return retention;
    }

    private long calculateActiveUsers(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<AuditLog> logs = auditService.getRecentLogsByPeriod(since);
        
        return logs.stream()
            .map(AuditLog::getUsername)
            .distinct()
            .count();
    }
} 