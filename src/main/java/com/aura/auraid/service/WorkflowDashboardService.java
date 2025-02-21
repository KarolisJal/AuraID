package com.aura.auraid.service;

import com.aura.auraid.dto.WorkflowDashboardDTO;
import com.aura.auraid.dto.PageResponseDTO;
import org.springframework.data.domain.Pageable;

public interface WorkflowDashboardService {
    // Admin dashboard methods
    WorkflowDashboardDTO getAdminDashboard();
    PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getAdminPendingRequests(Pageable pageable);
    PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getAllRequests(String status, String resourceType, Pageable pageable);
    PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO> getAdminActivities(Pageable pageable);
    WorkflowDashboardDTO.DashboardStatsDTO getAdminStats(String period);

    // User dashboard methods
    WorkflowDashboardDTO getUserDashboard(Long userId);
    PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getUserRequests(Long userId, String status, Pageable pageable);
    PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getUserPendingApprovals(Long userId, Pageable pageable);
    PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO> getUserActivities(Long userId, Pageable pageable);
    WorkflowDashboardDTO.DashboardStatsDTO getUserStats(Long userId, String period);

    // Common methods
    WorkflowDashboardDTO.WorkflowRequestSummaryDTO getRequestDetails(Long requestId, Long userId);
    PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO> getRequestActivities(Long requestId, Pageable pageable);
} 