package com.aura.auraid.service.impl;

import com.aura.auraid.model.PermissionType;
import com.aura.auraid.model.ResourcePermission;
import com.aura.auraid.repository.ResourcePermissionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionInitializationService {

    private final ResourcePermissionRepository permissionRepository;

    @PostConstruct
    @Transactional
    public void initializePermissions() {
        log.info("Initializing basic permissions...");
        
        for (PermissionType type : PermissionType.values()) {
            Long permissionId = (long) (type.ordinal() + 1);
            
            if (!permissionRepository.existsById(permissionId)) {
                ResourcePermission permission = new ResourcePermission();
                permission.setId(permissionId);
                permission.setName(type.name());
                permission.setDescription(getPermissionDescription(type));
                permission.setEnabled(true);
                permission.setCreatedBy(1L); // System user
                permission.setUpdatedBy(1L); // System user
                
                permissionRepository.save(permission);
                log.info("Created permission: {}", type.name());
            }
        }
        
        log.info("Permission initialization completed.");
    }

    private String getPermissionDescription(PermissionType type) {
        return switch (type) {
            case READ -> "Can view the resource";
            case WRITE -> "Can modify the resource";
            case DELETE -> "Can delete the resource";
            case ADMIN -> "Full control over the resource";
        };
    }
} 