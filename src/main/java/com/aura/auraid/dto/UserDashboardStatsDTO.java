package com.aura.auraid.dto;

import com.aura.auraid.model.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserDashboardStatsDTO {
    private LocalDateTime lastLogin;
    private LocalDateTime lastPasswordChange;
    private List<Map<String, Object>> activeDevices;
    private List<AuditLog> recentActivities;
    private Map<String, Object> activitySummary;
    private UserSecurityStatusDTO securityStatus;
} 