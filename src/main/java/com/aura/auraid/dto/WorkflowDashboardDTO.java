package com.aura.auraid.dto;

import lombok.Data;
import java.util.List;

@Data
public class WorkflowDashboardDTO {
    private DashboardStatsDTO stats;
    private List<WorkflowRequestSummaryDTO> recentRequests;
    private List<WorkflowActivityDTO> recentActivities;
    
    @Data
    public static class DashboardStatsDTO {
        private long pendingRequests;
        private long approvedRequests;
        private long rejectedRequests;
        private long totalRequests;
        private double averageApprovalTime; // in hours
    }
    
    @Data
    public static class WorkflowRequestSummaryDTO {
        private Long requestId;
        private String resourceName;
        private String resourceType;
        private String requesterName;
        private String workflowName;
        private String currentStepName;
        private int currentStepOrder;
        private int totalSteps;
        private String status;
        private String createdAt;
        private String timeAgo;
        private List<String> pendingApprovers;
        private List<ApproverActionDTO> recentActions;
    }
    
    @Data
    public static class WorkflowActivityDTO {
        private Long activityId;
        private String activityType; // e.g., "REQUEST_CREATED", "STEP_APPROVED", etc.
        private String description;
        private String actor;
        private String timeAgo;
        private String resourceName;
        private Long requestId;
    }
    
    @Data
    public static class ApproverActionDTO {
        private String approverName;
        private String action;
        private String comment;
        private String timeAgo;
        private String icon;
    }
} 