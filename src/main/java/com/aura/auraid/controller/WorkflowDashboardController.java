package com.aura.auraid.controller;

import com.aura.auraid.dto.WorkflowDashboardDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.service.WorkflowDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflow-dashboard")
@RequiredArgsConstructor
public class WorkflowDashboardController {

    private final WorkflowDashboardService dashboardService;

    // Admin dashboard endpoints
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowDashboardDTO> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/admin/pending-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO>> getAdminPendingRequests(
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getAdminPendingRequests(pageable));
    }

    @GetMapping("/admin/all-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO>> getAllRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resourceType,
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getAllRequests(status, resourceType, pageable));
    }

    @GetMapping("/admin/activities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO>> getAdminActivities(
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getAdminActivities(pageable));
    }

    // User dashboard endpoints
    @GetMapping("/user")
    public ResponseEntity<WorkflowDashboardDTO> getUserDashboard(
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(dashboardService.getUserDashboard(userId));
    }

    @GetMapping("/user/my-requests")
    public ResponseEntity<PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO>> getUserRequests(
            @RequestAttribute Long userId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getUserRequests(userId, status, pageable));
    }

    @GetMapping("/user/pending-approvals")
    public ResponseEntity<PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO>> getUserPendingApprovals(
            @RequestAttribute Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getUserPendingApprovals(userId, pageable));
    }

    @GetMapping("/user/activities")
    public ResponseEntity<PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO>> getUserActivities(
            @RequestAttribute Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getUserActivities(userId, pageable));
    }

    // Request detail endpoints
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getRequestDetails(
            @PathVariable Long requestId,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(dashboardService.getRequestDetails(requestId, userId));
    }

    @GetMapping("/requests/{requestId}/activities")
    public ResponseEntity<PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO>> getRequestActivities(
            @PathVariable Long requestId,
            Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getRequestActivities(requestId, pageable));
    }

    // Statistics endpoints
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkflowDashboardDTO.DashboardStatsDTO> getAdminStats(
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(dashboardService.getAdminStats(period));
    }

    @GetMapping("/user/stats")
    public ResponseEntity<WorkflowDashboardDTO.DashboardStatsDTO> getUserStats(
            @RequestAttribute Long userId,
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(dashboardService.getUserStats(userId, period));
    }
} 