package com.aura.auraid.controller;

import com.aura.auraid.model.AuditLog;
import com.aura.auraid.service.AuditService;
import com.aura.auraid.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class DashboardController {

    private final UserService userService;
    private final AuditService auditService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.getTotalUsers());
        stats.put("activeUsers", userService.getActiveUsers());
        stats.put("securityScore", calculateSecurityScore());
        stats.put("uptime", 99.9); // You might want to implement actual uptime monitoring

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity() {
        List<AuditLog> recentLogs = auditService.getRecentLogs(10);
        List<Map<String, Object>> activities = recentLogs.stream()
            .map(log -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", log.getId());
                activity.put("action", log.getAction());
                activity.put("username", log.getUsername());
                activity.put("entityType", log.getEntityType());
                activity.put("details", log.getDetails());
                activity.put("timestamp", log.getCreatedAt());
                activity.put("ipAddress", log.getIpAddress());
                activity.put("color", getColorForAction(log.getAction()));
                return activity;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(activities);
    }

    private int calculateSecurityScore() {
        // Implement your security score calculation logic
        return 85;
    }

    private String getColorForAction(String action) {
        // Map different actions to colors
        if (action.contains("LOGIN")) return "success.main";
        if (action.contains("CREATE")) return "info.main";
        if (action.contains("UPDATE")) return "warning.main";
        if (action.contains("DELETE")) return "error.main";
        if (action.contains("ERROR")) return "error.main";
        return "primary.main";
    }
} 