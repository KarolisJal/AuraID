package com.aura.auraid.repository;

import com.aura.auraid.model.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByWorkflowId(Long workflowId);
    Optional<ApprovalStep> findByWorkflowIdAndId(Long workflowId, Long id);
    List<ApprovalStep> findByWorkflowIdAndIdIn(Long workflowId, List<Long> ids);
    List<ApprovalStep> findByWorkflowIdOrderByStepOrder(Long workflowId);
} 