package com.aura.auraid.repository;

import com.aura.auraid.model.ApprovalWorkflow;
import com.aura.auraid.model.WorkflowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    List<ApprovalWorkflow> findByType(WorkflowType type);
    List<ApprovalWorkflow> findByActiveTrue();
} 