package com.aura.auraid.repository;

import com.aura.auraid.model.ApprovalAction;
import com.aura.auraid.model.ApprovalActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByStepExecutionId(Long stepExecutionId);
    List<ApprovalAction> findByStepExecutionIdAndAction(Long stepExecutionId, ApprovalActionType action);
    List<ApprovalAction> findByApproverId(Long approverId);
    
    // Basic queries
    Page<ApprovalAction> findByApproverId(Long approverId, Pageable pageable);
    
    // Recent activities
    List<ApprovalAction> findTop10ByOrderByActionTimeDesc();
    List<ApprovalAction> findTop10ByApproverIdOrderByActionTimeDesc(Long approverId);
    
    // Request-specific activities
    @Query("SELECT a FROM ApprovalAction a " +
           "WHERE a.stepExecution.accessRequest.id = :requestId " +
           "ORDER BY a.actionTime DESC")
    Page<ApprovalAction> findByAccessRequestId(@Param("requestId") Long requestId, Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COUNT(a) FROM ApprovalAction a " +
           "WHERE a.approver.id = :approverId AND a.action = :action")
    long countByApproverIdAndAction(@Param("approverId") Long approverId, @Param("action") ApprovalActionType action);
} 