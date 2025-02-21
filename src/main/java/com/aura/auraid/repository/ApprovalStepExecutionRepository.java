package com.aura.auraid.repository;

import com.aura.auraid.model.ApprovalStepExecution;
import com.aura.auraid.model.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalStepExecutionRepository extends JpaRepository<ApprovalStepExecution, Long> {
    List<ApprovalStepExecution> findByAccessRequestId(Long accessRequestId);
    List<ApprovalStepExecution> findByAccessRequestIdAndStatus(Long accessRequestId, ApprovalStatus status);
    List<ApprovalStepExecution> findByStepIdAndStatus(Long stepId, ApprovalStatus status);
} 