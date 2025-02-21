package com.aura.auraid.repository;

import com.aura.auraid.model.AccessRequest;
import com.aura.auraid.model.AccessRequestStatus;
import com.aura.auraid.model.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByRequesterId(Long requesterId);
    List<AccessRequest> findByStatus(AccessRequestStatus status);
    List<AccessRequest> findByRequesterIdAndStatus(Long requesterId, AccessRequestStatus status);
    List<AccessRequest> findByResourceId(Long resourceId);

    Page<AccessRequest> findByRequesterId(Long requesterId, Pageable pageable);
    Page<AccessRequest> findByStatus(AccessRequestStatus status, Pageable pageable);
    Page<AccessRequest> findByRequesterIdAndStatus(Long requesterId, AccessRequestStatus status, Pageable pageable);
    Page<AccessRequest> findByResourceId(Long resourceId, Pageable pageable);

    Page<AccessRequest> findByStatusAndResourceType(AccessRequestStatus status, ResourceType type, Pageable pageable);
    Page<AccessRequest> findByResourceType(ResourceType type, Pageable pageable);

    long countByStatus(AccessRequestStatus status);
    long countByStatusAndCreatedAtAfter(AccessRequestStatus status, LocalDateTime after);
    long countByCreatedAtAfter(LocalDateTime after);
    long countByRequesterIdAndStatus(Long requesterId, AccessRequestStatus status);
    long countByRequesterIdAndStatusAndCreatedAtAfter(Long requesterId, AccessRequestStatus status, LocalDateTime after);
    long countByRequesterIdAndCreatedAtAfter(Long requesterId, LocalDateTime after);

    List<AccessRequest> findTop10ByOrderByCreatedAtDesc();
    List<AccessRequest> findTop10ByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<AccessRequest> findByStatusAndCreatedAtAfter(AccessRequestStatus status, LocalDateTime after);
    List<AccessRequest> findByRequesterIdAndStatusAndCreatedAtAfter(Long requesterId, AccessRequestStatus status, LocalDateTime after);

    @Query("SELECT ar FROM AccessRequest ar " +
           "JOIN ar.approvalSteps step " +
           "JOIN step.step s " +
           "JOIN s.approvers approver " +
           "WHERE approver.id = :userId " +
           "AND step.status = 'PENDING' " +
           "AND ar.status = 'PENDING'")
    Page<AccessRequest> findPendingApprovalsByUserId(@Param("userId") Long userId, Pageable pageable);
} 