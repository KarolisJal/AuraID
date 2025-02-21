package com.aura.auraid.model;

public enum WorkflowType {
    SINGLE_APPROVER,           // Single manager approval
    SEQUENTIAL_MULTI_LEVEL,    // Multiple approvers in sequence
    PARALLEL_MULTI_LEVEL,      // Multiple approvers can approve simultaneously
    PERCENTAGE_APPROVAL,       // Requires percentage of approvers to approve
    UNANIMOUS_APPROVAL         // All approvers must approve
} 