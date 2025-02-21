package com.aura.auraid.controller;

import com.aura.auraid.dto.ApprovalWorkflowDTO;
import com.aura.auraid.dto.ApprovalStepDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.ApprovalActionType;
import com.aura.auraid.model.WorkflowType;
import com.aura.auraid.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService workflowService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalWorkflowDTO> createWorkflow(
            @Valid @RequestBody ApprovalWorkflowDTO workflowDTO,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(workflowService.createWorkflow(workflowDTO, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalWorkflowDTO> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalWorkflowDTO workflowDTO,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, workflowDTO, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalWorkflowDTO> getWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ApprovalWorkflowDTO>> getAllWorkflows(Pageable pageable) {
        return ResponseEntity.ok(workflowService.getAllWorkflows(pageable));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ApprovalWorkflowDTO>> getWorkflowsByType(
            @PathVariable WorkflowType type) {
        return ResponseEntity.ok(workflowService.getWorkflowsByType(type));
    }

    // Step management endpoints
    @PostMapping("/{workflowId}/steps")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalStepDTO> addStep(
            @PathVariable Long workflowId,
            @Valid @RequestBody ApprovalStepDTO stepDTO) {
        return ResponseEntity.ok(workflowService.addStep(workflowId, stepDTO));
    }

    @PutMapping("/{workflowId}/steps/{stepId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalStepDTO> updateStep(
            @PathVariable Long workflowId,
            @PathVariable Long stepId,
            @Valid @RequestBody ApprovalStepDTO stepDTO) {
        return ResponseEntity.ok(workflowService.updateStep(workflowId, stepId, stepDTO));
    }

    @DeleteMapping("/{workflowId}/steps/{stepId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStep(
            @PathVariable Long workflowId,
            @PathVariable Long stepId) {
        workflowService.deleteStep(workflowId, stepId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{workflowId}/steps/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderSteps(
            @PathVariable Long workflowId,
            @RequestBody List<Long> stepIds) {
        workflowService.reorderSteps(workflowId, stepIds);
        return ResponseEntity.ok().build();
    }

    // Workflow assignment endpoints
    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignWorkflow(
            @RequestParam Long resourceId,
            @RequestParam Long workflowId) {
        workflowService.assignWorkflowToResource(resourceId, workflowId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/assign/{resourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeWorkflow(@PathVariable Long resourceId) {
        workflowService.removeWorkflowFromResource(resourceId);
        return ResponseEntity.ok().build();
    }

    // Approval action endpoints
    @PostMapping("/steps/{stepExecutionId}/approve")
    public ResponseEntity<Void> approveStep(
            @PathVariable Long stepExecutionId,
            @RequestParam(required = false) String comment,
            @RequestAttribute Long userId) {
        workflowService.handleApprovalAction(stepExecutionId, userId, ApprovalActionType.APPROVE, comment);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/steps/{stepExecutionId}/reject")
    public ResponseEntity<Void> rejectStep(
            @PathVariable Long stepExecutionId,
            @RequestParam String comment,
            @RequestAttribute Long userId) {
        workflowService.handleApprovalAction(stepExecutionId, userId, ApprovalActionType.REJECT, comment);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/steps/{stepExecutionId}/request-changes")
    public ResponseEntity<Void> requestChanges(
            @PathVariable Long stepExecutionId,
            @RequestParam String comment,
            @RequestAttribute Long userId) {
        workflowService.handleApprovalAction(stepExecutionId, userId, ApprovalActionType.REQUEST_CHANGES, comment);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/steps/{stepExecutionId}/can-approve")
    public ResponseEntity<Boolean> canApprove(
            @PathVariable Long stepExecutionId,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(workflowService.canUserApprove(userId, stepExecutionId));
    }
} 