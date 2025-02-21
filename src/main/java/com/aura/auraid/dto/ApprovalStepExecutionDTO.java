package com.aura.auraid.dto;

import com.aura.auraid.model.ApprovalStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApprovalStepExecutionDTO {
    private Long id;
    private Long accessRequestId;
    private Long stepId;
    private String stepName;
    private ApprovalStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<ApprovalActionDTO> actions;
    
    // Additional fields for UI
    private String resourceName;
    private String requesterName;
    private String workflowName;
    private Integer stepOrder;
    private Integer totalSteps;
    private List<String> pendingApprovers;
    private List<String> completedApprovers;
} 