package com.aura.auraid.service.impl;

import com.aura.auraid.dto.ResourceDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.Resource;
import com.aura.auraid.model.ResourceType;
import com.aura.auraid.model.PermissionType;
import com.aura.auraid.model.ResourcePermission;
import com.aura.auraid.repository.ResourceRepository;
import com.aura.auraid.repository.ResourcePermissionRepository;
import com.aura.auraid.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {
    
    private final ResourceRepository resourceRepository;
    private final ResourcePermissionRepository permissionRepository;

    @Override
    @Transactional
    public ResourceDTO createResource(ResourceDTO resourceDTO, Long createdBy) {
        Resource resource = new Resource();
        resource.setName(resourceDTO.getName());
        resource.setDescription(resourceDTO.getDescription());
        resource.setType(resourceDTO.getType());
        resource.setPath(resourceDTO.getPath());
        resource.setCreatedBy(createdBy);
        resource.setUpdatedBy(createdBy);
        
        Resource savedResource = resourceRepository.save(resource);
        return mapToDTO(savedResource);
    }

    @Override
    @Transactional
    public ResourceDTO updateResource(Long id, ResourceDTO resourceDTO, Long updatedBy) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Resource not found with id: " + id));
        
        resource.setName(resourceDTO.getName());
        resource.setDescription(resourceDTO.getDescription());
        resource.setType(resourceDTO.getType());
        resource.setPath(resourceDTO.getPath());
        resource.setUpdatedBy(updatedBy);
        resource.setUpdatedAt(LocalDateTime.now());
        
        Resource updatedResource = resourceRepository.save(resource);
        return mapToDTO(updatedResource);
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new EntityNotFoundException("Resource not found with id: " + id);
        }
        resourceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceDTO getResource(Long id) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Resource not found with id: " + id));
        return mapToDTO(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ResourceDTO> getAllResources(Pageable pageable) {
        Page<Resource> resourcePage = resourceRepository.findAll(pageable);
        List<ResourceDTO> resources = resourcePage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
            
        return PageResponseDTO.of(
            resources,
            resourcePage.getNumber(),
            resourcePage.getSize(),
            resourcePage.getTotalElements(),
            resourcePage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceDTO> getResourcesByType(ResourceType type) {
        return resourceRepository.findByType(type).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceDTO> getResourcesByCreator(Long createdBy) {
        return resourceRepository.findByCreatedBy(createdBy).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, Long resourceId, String permissionName) {
        // First check if the resource exists
        if (!resourceRepository.existsById(resourceId)) {
            return false;
        }

        // Check if the user is the creator of the resource (creators have all permissions)
        Resource resource = resourceRepository.findById(resourceId).get();
        if (resource.getCreatedBy().equals(userId)) {
            return true;
        }

        // Check if the permission exists and is enabled
        try {
            PermissionType requestedPermission = PermissionType.valueOf(permissionName);
            Long permissionId = (long) (requestedPermission.ordinal() + 1);
            return permissionRepository.findById(permissionId)
                .map(ResourcePermission::isEnabled)
                .orElse(false);
        } catch (IllegalArgumentException e) {
            return false; // Invalid permission name
        }
    }

    private ResourceDTO mapToDTO(Resource resource) {
        ResourceDTO dto = new ResourceDTO();
        dto.setId(resource.getId());
        dto.setName(resource.getName());
        dto.setDescription(resource.getDescription());
        dto.setType(resource.getType());
        dto.setPath(resource.getPath());
        return dto;
    }
} 