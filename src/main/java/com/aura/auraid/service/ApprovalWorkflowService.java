package com.aura.auraid.service;

import com.aura.auraid.dto.ApprovalWorkflowDTO;
import com.aura.auraid.dto.ApprovalStepDTO;
import com.aura.auraid.model.ApprovalActionType;
import com.aura.auraid.model.WorkflowType;
import org.springframework.data.domain.Pageable;
import com.aura.auraid.dto.PageResponseDTO;

import java.util.List;

public interface ApprovalWorkflowService {
    // Workflow management
    ApprovalWorkflowDTO createWorkflow(ApprovalWorkflowDTO workflowDTO, Long createdBy);
    ApprovalWorkflowDTO updateWorkflow(Long id, ApprovalWorkflowDTO workflowDTO, Long updatedBy);
    void deleteWorkflow(Long id);
    ApprovalWorkflowDTO getWorkflow(Long id);
    PageResponseDTO<ApprovalWorkflowDTO> getAllWorkflows(Pageable pageable);
    List<ApprovalWorkflowDTO> getWorkflowsByType(WorkflowType type);

    // Step management
    ApprovalStepDTO addStep(Long workflowId, ApprovalStepDTO stepDTO);
    ApprovalStepDTO updateStep(Long workflowId, Long stepId, ApprovalStepDTO stepDTO);
    void deleteStep(Long workflowId, Long stepId);
    void reorderSteps(Long workflowId, List<Long> stepIds);

    // Workflow execution
    void processAccessRequest(Long accessRequestId);
    void handleApprovalAction(Long stepExecutionId, Long approverId, ApprovalActionType action, String comment);
    boolean canUserApprove(Long userId, Long stepExecutionId);
    
    // Workflow assignment
    void assignWorkflowToResource(Long resourceId, Long workflowId);
    void removeWorkflowFromResource(Long resourceId);
} 