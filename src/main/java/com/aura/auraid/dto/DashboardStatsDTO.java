package com.aura.auraid.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalUsers;
    private long activeUsers;
    private SecurityMetricsDTO securityMetrics;
    private Map<String, Object> userMetrics;
    private Map<String, Object> systemHealth;
} 