package com.aura.auraid.repository;

import com.aura.auraid.model.Resource;
import com.aura.auraid.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByType(ResourceType type);
    List<Resource> findByCreatedBy(Long userId);
    boolean existsByApprovalWorkflowId(Long workflowId);
} 