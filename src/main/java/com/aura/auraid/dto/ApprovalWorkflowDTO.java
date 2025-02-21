package com.aura.auraid.dto;

import com.aura.auraid.model.WorkflowType;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class ApprovalWorkflowDTO {
    private Long id;

    @NotBlank(message = "Workflow name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Workflow type is required")
    private WorkflowType type;

    private List<ApprovalStepDTO> steps;

    private boolean active = true;

    // Additional fields for response
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;
} 