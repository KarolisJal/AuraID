package com.aura.auraid.controller;

import com.aura.auraid.dto.UserDashboardStatsDTO;
import com.aura.auraid.dto.UserSecurityStatusDTO;
import com.aura.auraid.dto.UserActivityDTO;
import com.aura.auraid.service.AuditService;
import com.aura.auraid.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/user-dashboard")
@RequiredArgsConstructor
@Tag(name = "User Dashboard", description = "User dashboard endpoints for regular users")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class UserDashboardController {

    private final UserService userService;
    private final AuditService auditService;

    @GetMapping("/overview")
    @Operation(summary = "Get user dashboard overview", 
              description = "Retrieve user's personal dashboard statistics and information")
    public ResponseEntity<UserDashboardStatsDTO> getUserDashboardStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails.getUsername();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        Map<String, Object> activitySummary = auditService.getUserActivitySummary(username, thirtyDaysAgo);
        
        UserDashboardStatsDTO stats = UserDashboardStatsDTO.builder()
            .lastLogin(userService.getLastLoginTime(username))
            .lastPasswordChange(userService.getLastPasswordChangeTime(username))
            .activeDevices(userService.getActiveDevices(username))
            .recentActivities(auditService.getUserActivity(username, 5))
            .activitySummary(activitySummary)
            .securityStatus(buildUserSecurityStatus(username))
            .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/security-status")
    @Operation(summary = "Get user security status", 
              description = "Get detailed security status and recommendations")
    public ResponseEntity<UserSecurityStatusDTO> getSecurityStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails.getUsername();
        UserSecurityStatusDTO securityStatus = buildUserSecurityStatus(username);
        return ResponseEntity.ok(securityStatus);
    }

    @GetMapping("/activity")
    @Operation(summary = "Get user activity history", 
              description = "Retrieve user's activity history with optional filters")
    public ResponseEntity<List<UserActivityDTO>> getUserActivity(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
                LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
                LocalDateTime endDate,
            @RequestParam(defaultValue = "20") int limit) {
        
        String username = userDetails.getUsername();
        List<UserActivityDTO> activities = new ArrayList<>();
        
        if (startDate != null && endDate != null) {
            auditService.getAuditLogsForExport(startDate, endDate, username, null)
                .forEach(log -> activities.add(mapToUserActivityDTO(log)));
        } else {
            auditService.getUserActivity(username, limit)
                .forEach(log -> activities.add(mapToUserActivityDTO(log)));
        }
        
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/active-sessions")
    @Operation(summary = "Get active sessions", 
              description = "List all active sessions/devices for the user")
    public ResponseEntity<List<Map<String, Object>>> getActiveSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails.getUsername();
        return ResponseEntity.ok(userService.getActiveSessionsForUser(username));
    }

    @GetMapping("/security-recommendations")
    @Operation(summary = "Get security recommendations", 
              description = "Get personalized security recommendations for the user")
    public ResponseEntity<List<Map<String, Object>>> getSecurityRecommendations(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails.getUsername();
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // Check password age
        LocalDateTime lastPasswordChange = userService.getLastPasswordChangeTime(username);
        if (lastPasswordChange.isBefore(LocalDateTime.now().minusDays(90))) {
            recommendations.add(Map.of(
                "type", "PASSWORD_AGE",
                "severity", "MEDIUM",
                "message", "Your password is over 90 days old. Consider changing it.",
                "action", "CHANGE_PASSWORD"
            ));
        }
        
        // Check for recent suspicious activities
        List<Map<String, Object>> suspiciousActivities = userService.getRecentSuspiciousActivities(username);
        if (!suspiciousActivities.isEmpty()) {
            recommendations.add(Map.of(
                "type", "SUSPICIOUS_ACTIVITY",
                "severity", "HIGH",
                "message", "Suspicious activities detected from unknown locations.",
                "action", "REVIEW_ACTIVITIES",
                "details", suspiciousActivities
            ));
        }
        
        // Check MFA status
        if (!userService.isMfaEnabled(username)) {
            recommendations.add(Map.of(
                "type", "MFA_DISABLED",
                "severity", "HIGH",
                "message", "Enable two-factor authentication to improve account security.",
                "action", "ENABLE_MFA"
            ));
        }
        
        return ResponseEntity.ok(recommendations);
    }

    private UserSecurityStatusDTO buildUserSecurityStatus(String username) {
        return UserSecurityStatusDTO.builder()
            .passwordStrength(userService.getPasswordStrength(username))
            .mfaEnabled(userService.isMfaEnabled(username))
            .lastPasswordChange(userService.getLastPasswordChangeTime(username))
            .activeDevicesCount(userService.getActiveDevices(username).size())
            .recentSuspiciousActivities(userService.getRecentSuspiciousActivities(username))
            .securityScore(calculateUserSecurityScore(username))
            .build();
    }

    private int calculateUserSecurityScore(String username) {
        int score = 100;
        
        // Password strength (0-25 points)
        String passwordStrength = userService.getPasswordStrength(username);
        switch (passwordStrength.toLowerCase()) {
            case "weak": score -= 25; break;
            case "moderate": score -= 15; break;
            case "strong": break;
        }
        
        // MFA status (0-25 points)
        if (!userService.isMfaEnabled(username)) {
            score -= 25;
        }
        
        // Password age (0-15 points)
        LocalDateTime lastPasswordChange = userService.getLastPasswordChangeTime(username);
        long daysSinceChange = java.time.temporal.ChronoUnit.DAYS.between(
            lastPasswordChange, LocalDateTime.now());
        if (daysSinceChange > 180) score -= 15;
        else if (daysSinceChange > 90) score -= 10;
        
        // Recent suspicious activities (0-35 points)
        List<Map<String, Object>> suspiciousActivities = 
            userService.getRecentSuspiciousActivities(username);
        if (suspiciousActivities.size() > 5) score -= 35;
        else if (suspiciousActivities.size() > 2) score -= 25;
        else if (suspiciousActivities.size() > 0) score -= 15;
        
        return Math.max(0, score);
    }

    private UserActivityDTO mapToUserActivityDTO(com.aura.auraid.model.AuditLog log) {
        return UserActivityDTO.builder()
            .action(log.getAction())
            .timestamp(log.getCreatedAt())
            .ipAddress(log.getIpAddress())
            .userAgent(log.getUserAgent())
            .details(log.getDetails())
            .status(log.getDetails() != null && log.getDetails().contains("failed") ? 
                   "FAILED" : "SUCCESS")
            .build();
    }
}