package com.aura.auraid.controller;

import com.aura.auraid.model.UserActivity;
import com.aura.auraid.service.UserDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@Validated
public class UserDashboardController {

    @Autowired
    private UserDashboardService userDashboardService;

    @GetMapping("/user/{username}")
    public ResponseEntity<Map<String, Object>> getUserDashboard(@PathVariable String username) {
        return ResponseEntity.ok(userDashboardService.getDashboardData(username));
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<List<UserActivity>> getRecentActivity(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(userDashboardService.getRecentActivity(limit));
    }

    @GetMapping("/user/{username}/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics(
            @PathVariable String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(userDashboardService.getUserStatistics(username, since));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUserActivity(
            @RequestParam String username,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "10") @Min(1) @Max(1000) int limit) {
        if (limit > 1000) {
            return ResponseEntity.badRequest().body("Limit cannot exceed 1000");
        }
        return ResponseEntity.ok(userDashboardService.searchUserActivity(username, startDate, endDate, limit));
    }
} 