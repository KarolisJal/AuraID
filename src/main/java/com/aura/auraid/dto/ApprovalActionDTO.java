package com.aura.auraid.dto;

import com.aura.auraid.model.ApprovalActionType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApprovalActionDTO {
    private Long id;
    private Long stepExecutionId;
    private Long approverId;
    private ApprovalActionType action;
    private String comment;
    private LocalDateTime actionTime;
    
    // Additional fields for UI
    private String approverName;
    private String actionIcon;
    private String actionLabel;
    private String timeAgo;
} 