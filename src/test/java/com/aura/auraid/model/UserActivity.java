package com.aura.auraid.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserActivity {
    private String username;
    private LocalDateTime lastLogin;
    private Integer loginCount;
    private Integer failedLoginAttempts;
    private LocalDateTime lastActivity;
} 