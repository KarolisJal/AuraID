package com.aura.auraid.controller;

import com.aura.auraid.model.AuditLog;
import com.aura.auraid.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Audit", description = "Audit log management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/recent")
    @Operation(summary = "Get recent audit logs", description = "Retrieve the most recent audit logs with specified limit")
    public ResponseEntity<List<AuditLog>> getRecentLogs(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(auditService.getRecentLogs(limit));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get audit statistics", description = "Retrieve audit activity statistics since specified datetime")
    public ResponseEntity<Map<String, Long>> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getActivityStatistics(since));
    }

    @GetMapping("/failed-logins")
    @Operation(summary = "Get failed login attempts", description = "Retrieve recent failed login attempts with specified limit")
    public ResponseEntity<List<AuditLog>> getFailedLogins(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(auditService.getFailedLoginAttempts(limit));
    }

    @GetMapping("/suspicious")
    @Operation(summary = "Get suspicious activities", description = "Retrieve suspicious activities since specified datetime")
    public ResponseEntity<List<AuditLog>> getSuspiciousActivities(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getSuspiciousActivities(since));
    }

    @GetMapping("/failed-login-attempts")
    @Operation(summary = "Get recent failed login attempts by user", 
               description = "Retrieve count of failed login attempts grouped by username since specified datetime")
    public ResponseEntity<Map<String, Integer>> getFailedLoginAttempts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getRecentFailedLoginAttempts(since));
    }

    @GetMapping("/search")
    @Operation(summary = "Search audit logs", 
              description = "Search audit logs with various filters")
    public ResponseEntity<List<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @Pattern(regexp = "^[A-Z_]+$") String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(auditService.searchAuditLogs(username, action, entityType, ipAddress, startDate, endDate, limit));
    }

    @GetMapping("/user/{username}/activity-summary")
    @Operation(summary = "Get user activity summary", 
              description = "Get a summary of user's activities including most frequent actions, IP addresses, etc.")
    public ResponseEntity<Map<String, Object>> getUserActivitySummary(
            @PathVariable String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getUserActivitySummary(username, since));
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit logs", 
              description = "Export audit logs for a specific time period")
    public ResponseEntity<List<AuditLog>> exportAuditLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @Pattern(regexp = "^[A-Z_]+$") String action) {
        return ResponseEntity.ok(auditService.getAuditLogsForExport(startDate, endDate, username, action));
    }

    @GetMapping("/ip-activity")
    @Operation(summary = "Get IP address activity", 
              description = "Get activity summary grouped by IP address")
    public ResponseEntity<List<Map<String, Object>>> getIpAddressActivity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(auditService.getIpAddressActivity(since, limit));
    }

    @GetMapping("/action-trends")
    @Operation(summary = "Get action trends", 
              description = "Get trends of different actions over time periods")
    public ResponseEntity<Map<String, Map<String, Long>>> getActionTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getActionTrends(since));
    }
} 