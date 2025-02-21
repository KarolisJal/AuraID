package com.aura.auraid.service;

import com.aura.auraid.dto.ResourceDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.ResourceType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceService {
    ResourceDTO createResource(ResourceDTO resourceDTO, Long createdBy);
    ResourceDTO updateResource(Long id, ResourceDTO resourceDTO, Long updatedBy);
    void deleteResource(Long id);
    ResourceDTO getResource(Long id);
    PageResponseDTO<ResourceDTO> getAllResources(Pageable pageable);
    List<ResourceDTO> getResourcesByType(ResourceType type);
    List<ResourceDTO> getResourcesByCreator(Long createdBy);
    boolean hasAccess(Long userId, Long resourceId, String permissionName);
} 