package com.aura.auraid.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SecurityEventDTO {
    private String eventType;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String severity;
    private String description;
    private LocalDateTime timestamp;
    private boolean resolved;
} 