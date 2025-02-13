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
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM Metrics
        metrics.put("jvm", getDetailedJvmMetrics());
        
        // Response Time Metrics
        metrics.put("responseTime", getResponseTimeMetrics());
        
        // Error Rate Metrics
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
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        Map<String, Object> health = new HashMap<>();
        
        // Basic Status
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        
        // Runtime Metrics
        Map<String, Object> runtime = new HashMap<>();
        runtime.put("uptime", runtimeMXBean.getUptime());
        runtime.put("startTime", LocalDateTime.ofInstant(
            Instant.ofEpochMilli(runtimeMXBean.getStartTime()), 
            ZoneId.systemDefault()));
        health.put("runtime", runtime);
        
        // Memory Metrics
        Map<String, Object> memory = new HashMap<>();
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        
        memory.put("heapUsed", heapMemory.getUsed());
        memory.put("heapMax", heapMemory.getMax());
        memory.put("heapUtilization", (double) heapMemory.getUsed() / heapMemory.getMax() * 100);
        memory.put("nonHeapUsed", nonHeapMemory.getUsed());
        health.put("memory", memory);
        
        // Thread Metrics
        Map<String, Object> threads = new HashMap<>();
        threads.put("threadCount", threadMXBean.getThreadCount());
        threads.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        threads.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        health.put("threads", threads);
        
        // System Metrics
        Map<String, Object> system = new HashMap<>();
        system.put("availableProcessors", osMXBean.getAvailableProcessors());
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                (com.sun.management.OperatingSystemMXBean) osMXBean;
            system.put("systemCpuLoad", sunOsMXBean.getCpuLoad() * 100);
            system.put("processCpuLoad", sunOsMXBean.getProcessCpuLoad() * 100);
            system.put("freePhysicalMemory", sunOsMXBean.getFreeMemorySize());
            system.put("totalPhysicalMemory", sunOsMXBean.getTotalMemorySize());
        }
        health.put("system", system);
        
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
        
        // Garbage Collection Metrics
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Object> gcMetrics = new HashMap<>();
        gcBeans.forEach(gc -> {
            Map<String, Object> gcStats = new HashMap<>();
            gcStats.put("collectionCount", gc.getCollectionCount());
            gcStats.put("collectionTime", gc.getCollectionTime());
            gcMetrics.put(gc.getName(), gcStats);
        });
        jvmMetrics.put("gc", gcMetrics);
        
        // Class Loading Metrics
        ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
        Map<String, Object> classLoading = new HashMap<>();
        classLoading.put("loadedClassCount", classLoadingBean.getLoadedClassCount());
        classLoading.put("totalLoadedClassCount", classLoadingBean.getTotalLoadedClassCount());
        classLoading.put("unloadedClassCount", classLoadingBean.getUnloadedClassCount());
        jvmMetrics.put("classLoading", classLoading);
        
        return jvmMetrics;
    }

    private Map<String, Object> getResponseTimeMetrics() {
        // This would be implemented with actual response time tracking
        // For now, returning placeholder data
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("averageResponseTime", 150); // ms
        metrics.put("p95ResponseTime", 250); // ms
        metrics.put("p99ResponseTime", 500); // ms
        return metrics;
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