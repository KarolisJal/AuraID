package com.aura.auraid.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserSecurityStatusDTO {
    private String passwordStrength;
    private boolean mfaEnabled;
    private LocalDateTime lastPasswordChange;
    private int activeDevicesCount;
    private List<Map<String, Object>> recentSuspiciousActivities;
    private int securityScore;
} 