package com.aura.auraid.dto;

import com.aura.auraid.model.NotificationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private String referenceType;
    private Long referenceId;
    
    // Additional fields for UI display
    private String timeAgo;  // Formatted time string (e.g., "2 hours ago")
    private String icon;     // Icon identifier for the notification type
} 