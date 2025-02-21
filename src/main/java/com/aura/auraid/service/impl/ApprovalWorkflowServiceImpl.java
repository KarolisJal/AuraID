package com.aura.auraid.service.impl;

import com.aura.auraid.dto.ApprovalWorkflowDTO;
import com.aura.auraid.dto.ApprovalStepDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.*;
import com.aura.auraid.repository.*;
import com.aura.auraid.service.ApprovalWorkflowService;
import com.aura.auraid.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalStepRepository stepRepository;
    private final ResourceRepository resourceRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final ApprovalStepExecutionRepository stepExecutionRepository;
    private final ApprovalActionRepository actionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ApprovalWorkflowDTO createWorkflow(ApprovalWorkflowDTO workflowDTO, Long createdBy) {
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setName(workflowDTO.getName());
        workflow.setDescription(workflowDTO.getDescription());
        workflow.setType(workflowDTO.getType());
        workflow.setActive(true);
        workflow.setCreatedBy(createdBy);
        workflow.setUpdatedBy(createdBy);

        ApprovalWorkflow savedWorkflow = workflowRepository.save(workflow);

        // Create steps if provided
        if (workflowDTO.getSteps() != null) {
            List<ApprovalStep> steps = workflowDTO.getSteps().stream()
                .map(stepDTO -> createStep(savedWorkflow, stepDTO))
                .collect(Collectors.toList());
            savedWorkflow.setSteps(steps);
        }

        return mapToDTO(savedWorkflow);
    }

    @Override
    @Transactional
    public ApprovalWorkflowDTO updateWorkflow(Long id, ApprovalWorkflowDTO workflowDTO, Long updatedBy) {
        ApprovalWorkflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workflow not found"));

        workflow.setName(workflowDTO.getName());
        workflow.setDescription(workflowDTO.getDescription());
        workflow.setType(workflowDTO.getType());
        workflow.setUpdatedBy(updatedBy);
        workflow.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(workflowRepository.save(workflow));
    }

    @Override
    @Transactional
    public void deleteWorkflow(Long id) {
        ApprovalWorkflow workflow = workflowRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workflow not found"));
            
        // Check if workflow is in use
        if (resourceRepository.existsByApprovalWorkflowId(id)) {
            throw new IllegalStateException("Workflow is in use by resources and cannot be deleted");
        }
        
        workflowRepository.delete(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalWorkflowDTO getWorkflow(Long id) {
        return mapToDTO(workflowRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Workflow not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ApprovalWorkflowDTO> getAllWorkflows(Pageable pageable) {
        Page<ApprovalWorkflow> workflowPage = workflowRepository.findAll(pageable);
        List<ApprovalWorkflowDTO> workflows = workflowPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PageResponseDTO.of(
            workflows,
            workflowPage.getNumber(),
            workflowPage.getSize(),
            workflowPage.getTotalElements(),
            workflowPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalWorkflowDTO> getWorkflowsByType(WorkflowType type) {
        return workflowRepository.findByType(type).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApprovalStepDTO addStep(Long workflowId, ApprovalStepDTO stepDTO) {
        ApprovalWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new EntityNotFoundException("Workflow not found"));

        ApprovalStep step = createStep(workflow, stepDTO);
        return mapToStepDTO(stepRepository.save(step));
    }

    @Override
    @Transactional
    public ApprovalStepDTO updateStep(Long workflowId, Long stepId, ApprovalStepDTO stepDTO) {
        ApprovalStep step = stepRepository.findByWorkflowIdAndId(workflowId, stepId)
            .orElseThrow(() -> new EntityNotFoundException("Step not found"));

        step.setName(stepDTO.getName());
        step.setDescription(stepDTO.getDescription());
        step.setStepOrder(stepDTO.getStepOrder());
        step.setApprovalThreshold(stepDTO.getApprovalThreshold());
        step.setApprovers(mapUserReferences(stepDTO.getApproverIds()));

        return mapToStepDTO(stepRepository.save(step));
    }

    @Override
    @Transactional
    public void deleteStep(Long workflowId, Long stepId) {
        ApprovalStep step = stepRepository.findByWorkflowIdAndId(workflowId, stepId)
            .orElseThrow(() -> new EntityNotFoundException("Step not found"));
            
        stepRepository.delete(step);
    }

    @Override
    @Transactional
    public void reorderSteps(Long workflowId, List<Long> stepIds) {
        List<ApprovalStep> steps = stepRepository.findByWorkflowIdAndIdIn(workflowId, stepIds);
        Map<Long, ApprovalStep> stepMap = steps.stream()
            .collect(Collectors.toMap(ApprovalStep::getId, step -> step));

        for (int i = 0; i < stepIds.size(); i++) {
            ApprovalStep step = stepMap.get(stepIds.get(i));
            if (step != null) {
                step.setStepOrder(i + 1);
            }
        }

        stepRepository.saveAll(steps);
    }

    @Override
    @Transactional
    public void processAccessRequest(Long accessRequestId) {
        AccessRequest request = accessRequestRepository.findById(accessRequestId)
            .orElseThrow(() -> new EntityNotFoundException("Access request not found"));

        ApprovalWorkflow workflow = request.getResource().getApprovalWorkflow();
        if (workflow == null) {
            throw new IllegalStateException("Resource has no approval workflow configured");
        }

        // Initialize workflow execution if not started
        if (request.getApprovalSteps() == null || request.getApprovalSteps().isEmpty()) {
            initializeWorkflowExecution(request, workflow);
        }

        // Process current step
        processCurrentStep(request);
    }

    @Override
    @Transactional
    public void handleApprovalAction(Long stepExecutionId, Long approverId, ApprovalActionType action, String comment) {
        ApprovalStepExecution stepExecution = stepExecutionRepository.findById(stepExecutionId)
            .orElseThrow(() -> new EntityNotFoundException("Step execution not found"));

        validateApprover(stepExecution, approverId);

        // Record the action
        ApprovalAction approvalAction = new ApprovalAction();
        approvalAction.setStepExecution(stepExecution);
        approvalAction.setApprover(getUserReference(approverId));
        approvalAction.setAction(action);
        approvalAction.setComment(comment);
        actionRepository.save(approvalAction);

        // Update step status based on workflow type
        updateStepStatus(stepExecution);

        // Process next step if current is complete
        if (stepExecution.getStatus() == ApprovalStatus.APPROVED) {
            processAccessRequest(stepExecution.getAccessRequest().getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserApprove(Long userId, Long stepExecutionId) {
        ApprovalStepExecution stepExecution = stepExecutionRepository.findById(stepExecutionId)
            .orElseThrow(() -> new EntityNotFoundException("Step execution not found"));

        return stepExecution.getStep().getApprovers().stream()
            .anyMatch(approver -> approver.getId().equals(userId));
    }

    @Override
    @Transactional
    public void assignWorkflowToResource(Long resourceId, Long workflowId) {
        Resource resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new EntityNotFoundException("Resource not found"));
        ApprovalWorkflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new EntityNotFoundException("Workflow not found"));

        resource.setApprovalWorkflow(workflow);
        resourceRepository.save(resource);
    }

    @Override
    @Transactional
    public void removeWorkflowFromResource(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new EntityNotFoundException("Resource not found"));
        
        resource.setApprovalWorkflow(null);
        resourceRepository.save(resource);
    }

    private ApprovalStep createStep(ApprovalWorkflow workflow, ApprovalStepDTO stepDTO) {
        ApprovalStep step = new ApprovalStep();
        step.setWorkflow(workflow);
        step.setName(stepDTO.getName());
        step.setDescription(stepDTO.getDescription());
        step.setStepOrder(stepDTO.getStepOrder());
        step.setApprovalThreshold(stepDTO.getApprovalThreshold());
        step.setApprovers(mapUserReferences(stepDTO.getApproverIds()));
        step.setActive(true);
        return step;
    }

    private void initializeWorkflowExecution(AccessRequest request, ApprovalWorkflow workflow) {
        List<ApprovalStepExecution> stepExecutions = workflow.getSteps().stream()
            .map(step -> {
                ApprovalStepExecution execution = new ApprovalStepExecution();
                execution.setAccessRequest(request);
                execution.setStep(step);
                execution.setStatus(ApprovalStatus.PENDING);
                return execution;
            })
            .collect(Collectors.toList());

        request.setApprovalSteps(stepExecutions);
        request.setCurrentStepOrder(1);
        accessRequestRepository.save(request);
    }

    private void processCurrentStep(AccessRequest request) {
        ApprovalStepExecution currentStep = request.getApprovalSteps().stream()
            .filter(step -> step.getStep().getStepOrder() == request.getCurrentStepOrder())
            .findFirst()
            .orElse(null);

        if (currentStep == null) {
            return;
        }

        if (currentStep.getStatus() == ApprovalStatus.PENDING) {
            currentStep.setStatus(ApprovalStatus.IN_PROGRESS);
            stepExecutionRepository.save(currentStep);
            notifyApprovers(currentStep);
        } else if (currentStep.getStatus() == ApprovalStatus.APPROVED) {
            moveToNextStep(request);
        }
    }

    private void updateStepStatus(ApprovalStepExecution stepExecution) {
        WorkflowType workflowType = stepExecution.getStep().getWorkflow().getType();
        Set<ApprovalAction> actions = stepExecution.getApprovalActions();
        
        switch (workflowType) {
            case SINGLE_APPROVER:
                handleSingleApproverWorkflow(stepExecution, actions);
                break;
            case UNANIMOUS_APPROVAL:
                handleUnanimousApprovalWorkflow(stepExecution, actions);
                break;
            case PERCENTAGE_APPROVAL:
                handlePercentageApprovalWorkflow(stepExecution, actions);
                break;
            default:
                handleDefaultWorkflow(stepExecution, actions);
        }
    }

    private void handleSingleApproverWorkflow(ApprovalStepExecution stepExecution, Set<ApprovalAction> actions) {
        Optional<ApprovalAction> lastAction = actions.stream()
            .max(Comparator.comparing(ApprovalAction::getActionTime));
            
        lastAction.ifPresent(action -> {
            if (action.getAction() == ApprovalActionType.APPROVE) {
                stepExecution.setStatus(ApprovalStatus.APPROVED);
            } else if (action.getAction() == ApprovalActionType.REJECT) {
                stepExecution.setStatus(ApprovalStatus.REJECTED);
            }
        });
    }

    private void handleUnanimousApprovalWorkflow(ApprovalStepExecution stepExecution, Set<ApprovalAction> actions) {
        Set<Long> approvers = stepExecution.getStep().getApprovers().stream()
            .map(User::getId)
            .collect(Collectors.toSet());
            
        Set<Long> approvedBy = actions.stream()
            .filter(action -> action.getAction() == ApprovalActionType.APPROVE)
            .map(action -> action.getApprover().getId())
            .collect(Collectors.toSet());

        if (approvedBy.containsAll(approvers)) {
            stepExecution.setStatus(ApprovalStatus.APPROVED);
        }
    }

    private void handlePercentageApprovalWorkflow(ApprovalStepExecution stepExecution, Set<ApprovalAction> actions) {
        int totalApprovers = stepExecution.getStep().getApprovers().size();
        long approvalCount = actions.stream()
            .filter(action -> action.getAction() == ApprovalActionType.APPROVE)
            .count();

        int threshold = stepExecution.getStep().getApprovalThreshold();
        if (threshold > 0 && (approvalCount * 100 / totalApprovers) >= threshold) {
            stepExecution.setStatus(ApprovalStatus.APPROVED);
        }
    }

    private void handleDefaultWorkflow(ApprovalStepExecution stepExecution, Set<ApprovalAction> actions) {
        // Default to single approver behavior
        handleSingleApproverWorkflow(stepExecution, actions);
    }

    private void moveToNextStep(AccessRequest request) {
        int nextStepOrder = request.getCurrentStepOrder() + 1;
        boolean hasNextStep = request.getApprovalSteps().stream()
            .anyMatch(step -> step.getStep().getStepOrder() == nextStepOrder);

        if (hasNextStep) {
            request.setCurrentStepOrder(nextStepOrder);
            accessRequestRepository.save(request);
            processAccessRequest(request.getId());
        } else {
            // Workflow complete
            request.setStatus(AccessRequestStatus.APPROVED);
            accessRequestRepository.save(request);
        }
    }

    private void notifyApprovers(ApprovalStepExecution stepExecution) {
        String resourceName = stepExecution.getAccessRequest().getResource().getName();
        String requesterName = stepExecution.getAccessRequest().getRequester().getUsername();

        for (User approver : stepExecution.getStep().getApprovers()) {
            notificationService.createNotification(
                approver.getId(),
                "Approval Required",
                String.format("Access request for %s by %s requires your approval", resourceName, requesterName),
                NotificationType.ACCESS_REQUEST_SUBMITTED,
                "STEP_EXECUTION",
                stepExecution.getId()
            );
        }
    }

    private Set<User> mapUserReferences(Set<Long> userIds) {
        return userIds.stream()
            .map(this::getUserReference)
            .collect(Collectors.toSet());
    }

    private User getUserReference(Long userId) {
        return userRepository.getReferenceById(userId);
    }

    private ApprovalWorkflowDTO mapToDTO(ApprovalWorkflow workflow) {
        ApprovalWorkflowDTO dto = new ApprovalWorkflowDTO();
        dto.setId(workflow.getId());
        dto.setName(workflow.getName());
        dto.setDescription(workflow.getDescription());
        dto.setType(workflow.getType());
        dto.setActive(workflow.isActive());
        
        if (workflow.getSteps() != null) {
            dto.setSteps(workflow.getSteps().stream()
                .map(this::mapToStepDTO)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private ApprovalStepDTO mapToStepDTO(ApprovalStep step) {
        ApprovalStepDTO dto = new ApprovalStepDTO();
        dto.setId(step.getId());
        dto.setStepOrder(step.getStepOrder());
        dto.setName(step.getName());
        dto.setDescription(step.getDescription());
        dto.setApprovalThreshold(step.getApprovalThreshold());
        dto.setActive(step.isActive());
        dto.setWorkflowId(step.getWorkflow().getId());
        dto.setWorkflowName(step.getWorkflow().getName());
        
        Set<Long> approverIds = step.getApprovers().stream()
            .map(User::getId)
            .collect(Collectors.toSet());
        dto.setApproverIds(approverIds);
        
        return dto;
    }

    private void validateApprover(ApprovalStepExecution stepExecution, Long approverId) {
        boolean isApprover = stepExecution.getStep().getApprovers().stream()
            .anyMatch(approver -> approver.getId().equals(approverId));
            
        if (!isApprover) {
            throw new AccessDeniedException("User is not authorized to approve this step");
        }
        
        boolean hasAlreadyActed = stepExecution.getApprovalActions().stream()
            .anyMatch(action -> action.getApprover().getId().equals(approverId));
            
        if (hasAlreadyActed) {
            throw new IllegalStateException("User has already acted on this step");
        }
    }
} 