package com.aura.auraid.service.impl;

import com.aura.auraid.dto.AccessRequestDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.AccessRequest;
import com.aura.auraid.model.AccessRequestStatus;
import com.aura.auraid.model.Resource;
import com.aura.auraid.model.ResourcePermission;
import com.aura.auraid.model.PermissionType;
import com.aura.auraid.model.User;
import com.aura.auraid.repository.AccessRequestRepository;
import com.aura.auraid.repository.ResourceRepository;
import com.aura.auraid.service.AccessRequestService;
import com.aura.auraid.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service; 

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestServiceImpl implements AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;
    private final ResourceRepository resourceRepository;
    private final NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(AccessRequestServiceImpl.class);

    @Override
    @Transactional
    public AccessRequestDTO createRequest(AccessRequestDTO requestDTO, Long requesterId) {
        // Validate resource exists
        Resource resource = resourceRepository.findById(requestDTO.getResourceId())
            .orElseThrow(() -> new EntityNotFoundException("Resource not found"));

        // Create a temporary permission based on the enum type
        ResourcePermission permission = new ResourcePermission();
        permission.setId(requestDTO.getPermissionId());
        permission.setName(PermissionType.values()[requestDTO.getPermissionId().intValue() - 1].name());
        permission.setEnabled(true);

        AccessRequest request = new AccessRequest();
        request.setResource(resource);
        request.setPermission(permission);
        request.setRequester(getUserReference(requesterId));
        request.setStatus(AccessRequestStatus.PENDING);
        request.setJustification(requestDTO.getJustification());

        AccessRequest savedRequest = accessRequestRepository.save(request);
        
        // Notify resource owner about the new request
        notificationService.notifyAccessRequestSubmitted(
            savedRequest.getId(),
            requesterId,
            resource.getId()
        );

        return mapToDTO(savedRequest);
    }

    @Override
    @Transactional
    public AccessRequestDTO approveRequest(Long id, String comment, Long approverId) {
        AccessRequest request = getRequestAndValidateStatus(id);
        
        request.setStatus(AccessRequestStatus.APPROVED);
        request.setApprover(getUserReference(approverId));
        request.setApproverComment(comment);
        request.setApprovedAt(LocalDateTime.now());

        AccessRequest updatedRequest = accessRequestRepository.save(request);
        
        // Notify requester about approval
        notificationService.notifyAccessRequestApproved(
            updatedRequest.getId(),
            updatedRequest.getRequester().getId(),
            approverId
        );

        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public AccessRequestDTO rejectRequest(Long id, String comment, Long approverId) {
        AccessRequest request = getRequestAndValidateStatus(id);
        
        request.setStatus(AccessRequestStatus.REJECTED);
        request.setApprover(getUserReference(approverId));
        request.setApproverComment(comment);
        request.setApprovedAt(LocalDateTime.now());

        AccessRequest updatedRequest = accessRequestRepository.save(request);
        
        // Notify requester about rejection
        notificationService.notifyAccessRequestRejected(
            updatedRequest.getId(),
            updatedRequest.getRequester().getId(),
            approverId
        );

        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional
    public AccessRequestDTO cancelRequest(Long id, Long requesterId) {
        AccessRequest request = getRequestAndValidateStatus(id);
        
        // Validate that the cancellation is requested by the original requester
        if (!request.getRequester().getId().equals(requesterId)) {
            throw new IllegalStateException("Only the original requester can cancel the request");
        }

        request.setStatus(AccessRequestStatus.CANCELLED);
        return mapToDTO(accessRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public AccessRequestDTO getRequest(Long id) {
        return mapToDTO(accessRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Access request not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AccessRequestDTO> getPendingRequests(Pageable pageable) {
        return getRequestsByStatus(AccessRequestStatus.PENDING, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AccessRequestDTO> getRequestsByStatus(AccessRequestStatus status, Pageable pageable) {
        Page<AccessRequest> requestPage = accessRequestRepository.findByStatus(status, pageable);
        List<AccessRequestDTO> requests = requestPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PageResponseDTO.of(
            requests,
            requestPage.getNumber(),
            requestPage.getSize(),
            requestPage.getTotalElements(),
            requestPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AccessRequestDTO> getRequestsByRequester(Long requesterId, Pageable pageable) {
        Page<AccessRequest> requestPage = accessRequestRepository.findByRequesterId(requesterId, pageable);
        List<AccessRequestDTO> requests = requestPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PageResponseDTO.of(
            requests,
            requestPage.getNumber(),
            requestPage.getSize(),
            requestPage.getTotalElements(),
            requestPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AccessRequestDTO> getRequestsByResource(Long resourceId, Pageable pageable) {
        Page<AccessRequest> requestPage = accessRequestRepository.findByResourceId(resourceId, pageable);
        List<AccessRequestDTO> requests = requestPage.getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PageResponseDTO.of(
            requests,
            requestPage.getNumber(),
            requestPage.getSize(),
            requestPage.getTotalElements(),
            requestPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingRequests() {
        return accessRequestRepository.findByStatus(AccessRequestStatus.PENDING).size();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canApproveRequest(Long userId, Long requestId) {
        log.debug("canApproveRequest called with userId: {}, requestId: {}", userId, requestId);
        AccessRequest request = accessRequestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Access request not found"));
            
        // Check if user is the resource creator
        if (request.getResource().getCreatedBy().equals(userId)) {
            return true;
        }
        
        // Check if there's a workflow and if the user is an approver in the current step
        if (request.getApprovalSteps() != null && !request.getApprovalSteps().isEmpty()) {
            return request.getApprovalSteps().stream()
                .filter(step -> step.getStep().getStepOrder() == request.getCurrentStepOrder())
                .findFirst()
                .map(currentStep -> currentStep.getStep().getApprovers().stream()
                    .anyMatch(approver -> approver.getId().equals(userId)))
                .orElse(false);
        }
        
        return false;
    }

    private AccessRequest getRequestAndValidateStatus(Long id) {
        AccessRequest request = accessRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Access request not found"));
            
        if (request.getStatus() != AccessRequestStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING status");
        }
        
        return request;
    }

    private User getUserReference(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private AccessRequestDTO mapToDTO(AccessRequest request) {
        AccessRequestDTO dto = new AccessRequestDTO();
        dto.setId(request.getId());
        dto.setResourceId(request.getResource().getId());
        dto.setPermissionId(request.getPermission().getId());
        dto.setJustification(request.getJustification());
        dto.setStatus(request.getStatus());
        dto.setApproverComment(request.getApproverComment());
        dto.setApprovedAt(request.getApprovedAt());
        dto.setCreatedAt(request.getCreatedAt());
        
        // Set additional fields for response
        dto.setResourceName(request.getResource().getName());
        dto.setPermissionName(request.getPermission().getName());
        dto.setRequesterName(request.getRequester().getUsername());
        if (request.getApprover() != null) {
            dto.setApproverName(request.getApprover().getUsername());
        }
        
        return dto;
    }
} 