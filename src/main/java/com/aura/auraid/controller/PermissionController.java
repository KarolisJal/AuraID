package com.aura.auraid.controller;

import com.aura.auraid.dto.ResourcePermissionDTO;
import com.aura.auraid.model.PermissionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for managing simplified permissions.
 * Permission IDs map to PermissionType enum values as follows:
 * 1 = READ
 * 2 = WRITE
 * 3 = DELETE
 * 4 = ADMIN
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    @GetMapping
    public ResponseEntity<List<ResourcePermissionDTO>> getAllPermissions() {
        // Return the basic permission types with their corresponding IDs
        return ResponseEntity.ok(Arrays.stream(PermissionType.values())
                .map(type -> {
                    ResourcePermissionDTO dto = new ResourcePermissionDTO();
                    dto.setId((long) (type.ordinal() + 1)); // ID is 1-based index
                    dto.setName(type.name());
                    dto.setDescription(getPermissionDescription(type));
                    dto.setEnabled(true);
                    return dto;
                })
                .collect(Collectors.toList()));
    }

    @GetMapping("/resource/{resourceId}")
    public ResponseEntity<List<ResourcePermissionDTO>> getPermissionsForResource(
            @PathVariable Long resourceId) {
        // For our simplified system, all resources have the same permission types
        return getAllPermissions();
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