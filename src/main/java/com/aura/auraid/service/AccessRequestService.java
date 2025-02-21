package com.aura.auraid.service;

import com.aura.auraid.dto.AccessRequestDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.AccessRequestStatus;
import org.springframework.data.domain.Pageable;

public interface AccessRequestService {
    AccessRequestDTO createRequest(AccessRequestDTO requestDTO, Long requesterId);
    AccessRequestDTO approveRequest(Long id, String comment, Long approverId);
    AccessRequestDTO rejectRequest(Long id, String comment, Long approverId);
    AccessRequestDTO cancelRequest(Long id, Long requesterId);
    AccessRequestDTO getRequest(Long id);
    
    PageResponseDTO<AccessRequestDTO> getPendingRequests(Pageable pageable);
    PageResponseDTO<AccessRequestDTO> getRequestsByStatus(AccessRequestStatus status, Pageable pageable);
    PageResponseDTO<AccessRequestDTO> getRequestsByRequester(Long requesterId, Pageable pageable);
    PageResponseDTO<AccessRequestDTO> getRequestsByResource(Long resourceId, Pageable pageable);
    
    long countPendingRequests();
    boolean canApproveRequest(Long userId, Long requestId);
} 