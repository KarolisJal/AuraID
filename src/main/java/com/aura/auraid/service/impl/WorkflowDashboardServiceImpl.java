package com.aura.auraid.service.impl;

import com.aura.auraid.dto.WorkflowDashboardDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.*;
import com.aura.auraid.repository.*;
import com.aura.auraid.service.WorkflowDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowDashboardServiceImpl implements WorkflowDashboardService {

    private final AccessRequestRepository accessRequestRepository;
    private final ApprovalStepExecutionRepository stepExecutionRepository;
    private final ApprovalActionRepository actionRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkflowDashboardDTO getAdminDashboard() {
        WorkflowDashboardDTO dashboard = new WorkflowDashboardDTO();
        dashboard.setStats(getAdminStats("today"));
        dashboard.setRecentRequests(getRecentRequests(10));
        dashboard.setRecentActivities(getRecentAdminActivities(10));
        return dashboard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getAdminPendingRequests(Pageable pageable) {
        Page<AccessRequest> requests = accessRequestRepository.findByStatus(AccessRequestStatus.PENDING, pageable);
        return createRequestSummaryPage(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getAllRequests(
            String status, String resourceType, Pageable pageable) {
        Page<AccessRequest> requests;
        if (status != null && resourceType != null) {
            requests = accessRequestRepository.findByStatusAndResourceType(
                AccessRequestStatus.valueOf(status),
                ResourceType.valueOf(resourceType),
                pageable
            );
        } else if (status != null) {
            requests = accessRequestRepository.findByStatus(AccessRequestStatus.valueOf(status), pageable);
        } else if (resourceType != null) {
            requests = accessRequestRepository.findByResourceType(ResourceType.valueOf(resourceType), pageable);
        } else {
            requests = accessRequestRepository.findAll(pageable);
        }
        return createRequestSummaryPage(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO> getAdminActivities(Pageable pageable) {
        Page<ApprovalAction> actions = actionRepository.findAll(pageable);
        return createActivityPage(actions);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDashboardDTO.DashboardStatsDTO getAdminStats(String period) {
        LocalDateTime startDate = getStartDateForPeriod(period);
        
        WorkflowDashboardDTO.DashboardStatsDTO stats = new WorkflowDashboardDTO.DashboardStatsDTO();
        stats.setPendingRequests(accessRequestRepository.countByStatus(AccessRequestStatus.PENDING));
        stats.setApprovedRequests(accessRequestRepository.countByStatusAndCreatedAtAfter(AccessRequestStatus.APPROVED, startDate));
        stats.setRejectedRequests(accessRequestRepository.countByStatusAndCreatedAtAfter(AccessRequestStatus.REJECTED, startDate));
        stats.setTotalRequests(accessRequestRepository.countByCreatedAtAfter(startDate));
        stats.setAverageApprovalTime(calculateAverageApprovalTime(startDate));
        
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDashboardDTO getUserDashboard(Long userId) {
        WorkflowDashboardDTO dashboard = new WorkflowDashboardDTO();
        dashboard.setStats(getUserStats(userId, "today"));
        dashboard.setRecentRequests(getUserRecentRequests(userId, 10));
        dashboard.setRecentActivities(getUserRecentActivities(userId, 10));
        return dashboard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getUserRequests(
            Long userId, String status, Pageable pageable) {
        Page<AccessRequest> requests;
        if (status != null) {
            requests = accessRequestRepository.findByRequesterIdAndStatus(
                userId,
                AccessRequestStatus.valueOf(status),
                pageable
            );
        } else {
            requests = accessRequestRepository.findByRequesterId(userId, pageable);
        }
        return createRequestSummaryPage(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getUserPendingApprovals(
            Long userId, Pageable pageable) {
        Page<AccessRequest> requests = accessRequestRepository.findPendingApprovalsByUserId(userId, pageable);
        return createRequestSummaryPage(requests);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO> getUserActivities(
            Long userId, Pageable pageable) {
        Page<ApprovalAction> actions = actionRepository.findByApproverId(userId, pageable);
        return createActivityPage(actions);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDashboardDTO.DashboardStatsDTO getUserStats(Long userId, String period) {
        LocalDateTime startDate = getStartDateForPeriod(period);
        
        WorkflowDashboardDTO.DashboardStatsDTO stats = new WorkflowDashboardDTO.DashboardStatsDTO();
        stats.setPendingRequests(accessRequestRepository.countByRequesterIdAndStatus(userId, AccessRequestStatus.PENDING));
        stats.setApprovedRequests(accessRequestRepository.countByRequesterIdAndStatusAndCreatedAtAfter(
            userId, AccessRequestStatus.APPROVED, startDate));
        stats.setRejectedRequests(accessRequestRepository.countByRequesterIdAndStatusAndCreatedAtAfter(
            userId, AccessRequestStatus.REJECTED, startDate));
        stats.setTotalRequests(accessRequestRepository.countByRequesterIdAndCreatedAtAfter(userId, startDate));
        stats.setAverageApprovalTime(calculateUserAverageApprovalTime(userId, startDate));
        
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDashboardDTO.WorkflowRequestSummaryDTO getRequestDetails(Long requestId, Long userId) {
        AccessRequest request = accessRequestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Access request not found"));
            
        // Check access
        if (!request.getRequester().getId().equals(userId) && 
            !isUserApprover(userId, request)) {
            throw new AccessDeniedException("You don't have access to this request");
        }
        
        return createRequestSummary(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO> getRequestActivities(
            Long requestId, Pageable pageable) {
        Page<ApprovalAction> actions = actionRepository.findByAccessRequestId(requestId, pageable);
        return createActivityPage(actions);
    }

    private List<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getRecentRequests(int limit) {
        return accessRequestRepository.findTop10ByOrderByCreatedAtDesc().stream()
            .map(this::createRequestSummary)
            .collect(Collectors.toList());
    }

    private List<WorkflowDashboardDTO.WorkflowActivityDTO> getRecentAdminActivities(int limit) {
        return actionRepository.findTop10ByOrderByActionTimeDesc().stream()
            .map(this::createActivityDTO)
            .collect(Collectors.toList());
    }

    private List<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> getUserRecentRequests(Long userId, int limit) {
        return accessRequestRepository.findTop10ByRequesterIdOrderByCreatedAtDesc(userId).stream()
            .map(this::createRequestSummary)
            .collect(Collectors.toList());
    }

    private List<WorkflowDashboardDTO.WorkflowActivityDTO> getUserRecentActivities(Long userId, int limit) {
        return actionRepository.findTop10ByApproverIdOrderByActionTimeDesc(userId).stream()
            .map(this::createActivityDTO)
            .collect(Collectors.toList());
    }

    private WorkflowDashboardDTO.WorkflowRequestSummaryDTO createRequestSummary(AccessRequest request) {
        WorkflowDashboardDTO.WorkflowRequestSummaryDTO summary = new WorkflowDashboardDTO.WorkflowRequestSummaryDTO();
        summary.setRequestId(request.getId());
        summary.setResourceName(request.getResource().getName());
        summary.setResourceType(request.getResource().getType().toString());
        summary.setRequesterName(request.getRequester().getUsername());
        summary.setWorkflowName(request.getResource().getApprovalWorkflow().getName());
        
        ApprovalStepExecution currentStep = getCurrentStep(request);
        if (currentStep != null) {
            summary.setCurrentStepName(currentStep.getStep().getName());
            summary.setCurrentStepOrder(currentStep.getStep().getStepOrder());
            summary.setTotalSteps(request.getApprovalSteps().size());
            summary.setPendingApprovers(getPendingApprovers(currentStep));
        }
        
        summary.setStatus(request.getStatus().toString());
        summary.setCreatedAt(request.getCreatedAt().toString());
        summary.setTimeAgo(calculateTimeAgo(request.getCreatedAt()));
        summary.setRecentActions(getRecentActions(request));
        
        return summary;
    }

    private WorkflowDashboardDTO.WorkflowActivityDTO createActivityDTO(ApprovalAction action) {
        WorkflowDashboardDTO.WorkflowActivityDTO activity = new WorkflowDashboardDTO.WorkflowActivityDTO();
        activity.setActivityId(action.getId());
        activity.setActivityType(action.getAction().toString());
        activity.setActor(action.getApprover().getUsername());
        activity.setTimeAgo(calculateTimeAgo(action.getActionTime()));
        
        AccessRequest request = action.getStepExecution().getAccessRequest();
        activity.setResourceName(request.getResource().getName());
        activity.setRequestId(request.getId());
        
        activity.setDescription(createActivityDescription(action));
        
        return activity;
    }

    private String createActivityDescription(ApprovalAction action) {
        String actorName = action.getApprover().getUsername();
        String resourceName = action.getStepExecution().getAccessRequest().getResource().getName();
        
        return switch (action.getAction()) {
            case APPROVE -> String.format("%s approved access to %s", actorName, resourceName);
            case REJECT -> String.format("%s rejected access to %s", actorName, resourceName);
            case REQUEST_CHANGES -> String.format("%s requested changes for %s", actorName, resourceName);
            case DELEGATE -> String.format("%s delegated approval for %s", actorName, resourceName);
            case ESCALATE -> String.format("%s escalated request for %s", actorName, resourceName);
            case COMMENT -> String.format("%s commented on request for %s", actorName, resourceName);
            default -> String.format("%s performed action on %s", actorName, resourceName);
        };
    }

    private List<String> getPendingApprovers(ApprovalStepExecution step) {
        Set<Long> approvedBy = step.getApprovalActions().stream()
            .map(action -> action.getApprover().getId())
            .collect(Collectors.toSet());
            
        return step.getStep().getApprovers().stream()
            .filter(approver -> !approvedBy.contains(approver.getId()))
            .map(User::getUsername)
            .collect(Collectors.toList());
    }

    private List<WorkflowDashboardDTO.ApproverActionDTO> getRecentActions(AccessRequest request) {
        return request.getApprovalSteps().stream()
            .flatMap(step -> step.getApprovalActions().stream())
            .sorted((a1, a2) -> a2.getActionTime().compareTo(a1.getActionTime()))
            .limit(5)
            .map(this::createApproverActionDTO)
            .collect(Collectors.toList());
    }

    private WorkflowDashboardDTO.ApproverActionDTO createApproverActionDTO(ApprovalAction action) {
        WorkflowDashboardDTO.ApproverActionDTO dto = new WorkflowDashboardDTO.ApproverActionDTO();
        dto.setApproverName(action.getApprover().getUsername());
        dto.setAction(action.getAction().toString());
        dto.setComment(action.getComment());
        dto.setTimeAgo(calculateTimeAgo(action.getActionTime()));
        dto.setIcon(getActionIcon(action.getAction()));
        return dto;
    }

    private String getActionIcon(ApprovalActionType action) {
        return switch (action) {
            case APPROVE -> "check_circle";
            case REJECT -> "cancel";
            case REQUEST_CHANGES -> "edit";
            case DELEGATE -> "person_add";
            case ESCALATE -> "priority_high";
            case COMMENT -> "comment";
        };
    }

    private ApprovalStepExecution getCurrentStep(AccessRequest request) {
        return request.getApprovalSteps().stream()
            .filter(step -> step.getStep().getStepOrder() == request.getCurrentStepOrder())
            .findFirst()
            .orElse(null);
    }

    private boolean isUserApprover(Long userId, AccessRequest request) {
        return request.getApprovalSteps().stream()
            .anyMatch(step -> step.getStep().getApprovers().stream()
                .anyMatch(approver -> approver.getId().equals(userId)));
    }

    private LocalDateTime getStartDateForPeriod(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period != null ? period.toLowerCase() : "all") {
            case "today" -> now.truncatedTo(ChronoUnit.DAYS);
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> now.minusYears(100); // Effectively all time
        };
    }

    private double calculateAverageApprovalTime(LocalDateTime since) {
        List<AccessRequest> approvedRequests = accessRequestRepository
            .findByStatusAndCreatedAtAfter(AccessRequestStatus.APPROVED, since);
            
        if (approvedRequests.isEmpty()) {
            return 0.0;
        }
        
        double totalHours = approvedRequests.stream()
            .mapToDouble(request -> 
                ChronoUnit.HOURS.between(request.getCreatedAt(), request.getApprovedAt()))
            .sum();
            
        return totalHours / approvedRequests.size();
    }

    private double calculateUserAverageApprovalTime(Long userId, LocalDateTime since) {
        List<AccessRequest> approvedRequests = accessRequestRepository
            .findByRequesterIdAndStatusAndCreatedAtAfter(userId, AccessRequestStatus.APPROVED, since);
            
        if (approvedRequests.isEmpty()) {
            return 0.0;
        }
        
        double totalHours = approvedRequests.stream()
            .mapToDouble(request -> 
                ChronoUnit.HOURS.between(request.getCreatedAt(), request.getApprovedAt()))
            .sum();
            
        return totalHours / approvedRequests.size();
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        
        if (minutes < 60) {
            return minutes + " minutes ago";
        }
        
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return hours + " hours ago";
        }
        
        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 30) {
            return days + " days ago";
        }
        
        long months = ChronoUnit.MONTHS.between(dateTime, now);
        if (months < 12) {
            return months + " months ago";
        }
        
        return ChronoUnit.YEARS.between(dateTime, now) + " years ago";
    }

    private PageResponseDTO<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> createRequestSummaryPage(
            Page<AccessRequest> requests) {
        List<WorkflowDashboardDTO.WorkflowRequestSummaryDTO> summaries = requests.getContent().stream()
            .map(this::createRequestSummary)
            .collect(Collectors.toList());
            
        return PageResponseDTO.of(
            summaries,
            requests.getNumber(),
            requests.getSize(),
            requests.getTotalElements(),
            requests.getTotalPages()
        );
    }

    private PageResponseDTO<WorkflowDashboardDTO.WorkflowActivityDTO> createActivityPage(
            Page<ApprovalAction> actions) {
        List<WorkflowDashboardDTO.WorkflowActivityDTO> activities = actions.getContent().stream()
            .map(this::createActivityDTO)
            .collect(Collectors.toList());
            
        return PageResponseDTO.of(
            activities,
            actions.getNumber(),
            actions.getSize(),
            actions.getTotalElements(),
            actions.getTotalPages()
        );
    }
} 