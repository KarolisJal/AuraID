package com.aura.auraid.controller;

import com.aura.auraid.dto.ResourceDTO;
import com.aura.auraid.dto.ResourcePermissionDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.ResourceType;
import com.aura.auraid.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceDTO> createResource(
            @Valid @RequestBody ResourceDTO resourceDTO,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(resourceService.createResource(resourceDTO, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceDTO> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceDTO resourceDTO,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(resourceService.updateResource(id, resourceDTO, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceDTO> getResource(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResource(id));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ResourceDTO>> getAllResources(Pageable pageable) {
        return ResponseEntity.ok(resourceService.getAllResources(pageable));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ResourceDTO>> getResourcesByType(
            @PathVariable ResourceType type) {
        return ResponseEntity.ok(resourceService.getResourcesByType(type));
    }

    @GetMapping("/creator/{createdBy}")
    public ResponseEntity<List<ResourceDTO>> getResourcesByCreator(
            @PathVariable Long createdBy) {
        return ResponseEntity.ok(resourceService.getResourcesByCreator(createdBy));
    }

    @GetMapping("/{resourceId}/access/{permissionName}")
    public ResponseEntity<Boolean> checkAccess(
            @PathVariable Long resourceId,
            @PathVariable String permissionName,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(resourceService.hasAccess(userId, resourceId, permissionName));
    }
} 