package com.aura.auraid.service;

import com.aura.auraid.dto.NotificationDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    NotificationDTO createNotification(Long userId, String title, String message, 
                                     NotificationType type, String referenceType, Long referenceId);
    
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    
    PageResponseDTO<NotificationDTO> getUserNotifications(Long userId, Pageable pageable);
    PageResponseDTO<NotificationDTO> getUnreadNotifications(Long userId, Pageable pageable);
    
    long countUnreadNotifications(Long userId);
    
    // Convenience methods for common notifications
    void notifyAccessRequestSubmitted(Long requestId, Long requesterId, Long resourceId);
    void notifyAccessRequestApproved(Long requestId, Long requesterId, Long approverId);
    void notifyAccessRequestRejected(Long requestId, Long requesterId, Long approverId);
} 