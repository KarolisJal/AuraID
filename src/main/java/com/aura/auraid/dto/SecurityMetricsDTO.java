package com.aura.auraid.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SecurityMetricsDTO {
    private int securityScore;
    private long loginAttempts;
    private long successfulLogins;
    private long failedLogins;
    private long registrationAttempts;
    private long successfulRegistrations;
    private long failedRegistrations;
    private LocalDateTime lastUpdated;
} 