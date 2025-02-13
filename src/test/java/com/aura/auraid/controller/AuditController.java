package com.aura.auraid.controller;

import com.aura.auraid.model.AuditLog;
import com.aura.auraid.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private static final Set<String> VALID_ACTIONS = Set.of(
        "LOGIN", "LOGOUT", "LOGIN_FAILED", "PASSWORD_CHANGE", "PROFILE_UPDATE",
        "SUSPICIOUS_LOGIN", "MFA_ENABLED", "MFA_DISABLED"
    );

    @Autowired
    private AuditService auditService;

    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> getRecentLogs() {
        return ResponseEntity.ok(auditService.getRecentLogs(10));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getActivityStatistics(since));
    }

    @GetMapping("/failed-logins")
    public ResponseEntity<List<AuditLog>> getFailedLogins() {
        return ResponseEntity.ok(auditService.getFailedLoginAttempts(10));
    }

    @GetMapping("/suspicious")
    public ResponseEntity<List<AuditLog>> getSuspiciousActivities(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getSuspiciousActivities(since));
    }

    @GetMapping("/failed-login-attempts")
    public ResponseEntity<Map<String, Integer>> getFailedLoginAttempts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getRecentFailedLoginAttempts(since));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String ipAddress,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") int limit) {
        
        if (action != null && !VALID_ACTIONS.contains(action)) {
            return ResponseEntity.badRequest().body("Invalid action specified");
        }

        List<AuditLog> logs = auditService.searchAuditLogs(
            username, action, entityType, ipAddress, startDate, endDate, limit);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{username}/activity-summary")
    public ResponseEntity<Map<String, Object>> getUserActivitySummary(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(auditService.getUserActivitySummary(username, since));
    }
} 