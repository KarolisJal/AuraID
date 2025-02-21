package com.aura.auraid.controller;

import com.aura.auraid.dto.AccessRequestDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.AccessRequestStatus;
import com.aura.auraid.service.AccessRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/access-requests")
@RequiredArgsConstructor
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    @PostMapping
    public ResponseEntity<AccessRequestDTO> createRequest(
            @Valid @RequestBody AccessRequestDTO requestDTO,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(accessRequestService.createRequest(requestDTO, userId));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or @accessRequestService.canApproveRequest(authentication.principal.id, #id)")
    public ResponseEntity<AccessRequestDTO> approveRequest(
            @PathVariable Long id,
            @RequestParam String comment,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(accessRequestService.approveRequest(id, comment, userId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or @accessRequestService.canApproveRequest(authentication.principal.id, #id)")
    public ResponseEntity<AccessRequestDTO> rejectRequest(
            @PathVariable Long id,
            @RequestParam String comment,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(accessRequestService.rejectRequest(id, comment, userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AccessRequestDTO> cancelRequest(
            @PathVariable Long id,
            @RequestAttribute Long userId) {
        return ResponseEntity.ok(accessRequestService.cancelRequest(id, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessRequestDTO> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(accessRequestService.getRequest(id));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<AccessRequestDTO>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(accessRequestService.getPendingRequests(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<AccessRequestDTO>> getRequestsByStatus(
            @PathVariable AccessRequestStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(accessRequestService.getRequestsByStatus(status, pageable));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<PageResponseDTO<AccessRequestDTO>> getMyRequests(
            @RequestAttribute Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(accessRequestService.getRequestsByRequester(userId, pageable));
    }

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponseDTO<AccessRequestDTO>> getRequestsByResource(
            @PathVariable Long resourceId,
            Pageable pageable) {
        return ResponseEntity.ok(accessRequestService.getRequestsByResource(resourceId, pageable));
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countPendingRequests() {
        return ResponseEntity.ok(accessRequestService.countPendingRequests());
    }
} 