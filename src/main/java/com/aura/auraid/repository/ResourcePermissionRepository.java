package com.aura.auraid.repository;

import com.aura.auraid.model.ResourcePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, Long> {
} 