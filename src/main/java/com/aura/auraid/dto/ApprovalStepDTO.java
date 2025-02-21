package com.aura.auraid.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Data
public class ApprovalStepDTO {
    private Long id;

    @NotNull(message = "Step order is required")
    private Integer stepOrder;

    @NotBlank(message = "Step name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Integer approvalThreshold;

    @NotNull(message = "At least one approver is required")
    private Set<Long> approverIds;

    private boolean active = true;

    // Additional fields for response
    private Set<UserDTO> approvers;
    private String workflowName;
    private Long workflowId;
} 