package com.aura.auraid.model;

public enum ApprovalActionType {
    APPROVE,
    REJECT,
    REQUEST_CHANGES,
    DELEGATE,           // Delegate approval to another user
    ESCALATE,          // Escalate to higher authority
    COMMENT            // Just adding a comment without decision
} 