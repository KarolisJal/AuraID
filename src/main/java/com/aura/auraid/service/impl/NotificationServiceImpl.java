package com.aura.auraid.service.impl;

import com.aura.auraid.dto.NotificationDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.model.Notification;
import com.aura.auraid.model.NotificationType;
import com.aura.auraid.model.Resource;
import com.aura.auraid.model.User;
import com.aura.auraid.repository.NotificationRepository;
import com.aura.auraid.repository.ResourceRepository;
import com.aura.auraid.service.NotificationService;
import com.aura.auraid.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ResourceRepository resourceRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    @Transactional
    public NotificationDTO createNotification(Long userId, String title, String message,
                                            NotificationType type, String referenceType, Long referenceId) {
        Notification notification = new Notification();
        notification.setUser(getUserReference(userId));
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setRead(false);

        NotificationDTO notificationDTO = mapToDTO(notificationRepository.save(notification));
        
        // Send real-time notification via WebSocket
        webSocketNotificationService.sendNotificationToUser(userId, notificationDTO);
        
        // Update unread count
        long unreadCount = countUnreadNotifications(userId);
        webSocketNotificationService.sendUnreadCountToUser(userId, unreadCount);

        return notificationDTO;
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
            
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        // Update unread count via WebSocket
        long unreadCount = countUnreadNotifications(notification.getUser().getId());
        webSocketNotificationService.sendUnreadCountToUser(notification.getUser().getId(), unreadCount);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository
            .findByUserIdAndReadOrderByCreatedAtDesc(userId, false);
            
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(unreadNotifications);
        
        // Update unread count via WebSocket
        webSocketNotificationService.sendUnreadCountToUser(userId, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<NotificationDTO> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findAll(pageable);
        List<NotificationDTO> notifications = notificationPage.getContent().stream()
            .filter(notification -> notification.getUser().getId().equals(userId))
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PageResponseDTO.of(
            notifications,
            notificationPage.getNumber(),
            notificationPage.getSize(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<NotificationDTO> getUnreadNotifications(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findAll(pageable);
        List<NotificationDTO> notifications = notificationPage.getContent().stream()
            .filter(notification -> notification.getUser().getId().equals(userId) && !notification.isRead())
            .map(this::mapToDTO)
            .collect(Collectors.toList());

        return PageResponseDTO.of(
            notifications,
            notificationPage.getNumber(),
            notificationPage.getSize(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndRead(userId, false);
    }

    @Override
    @Transactional
    public void notifyAccessRequestSubmitted(Long requestId, Long requesterId, Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new EntityNotFoundException("Resource not found"));
            
        // Notify resource owner
        createNotification(
            resource.getCreatedBy(),
            "New Access Request",
            "A new access request has been submitted for " + resource.getName(),
            NotificationType.ACCESS_REQUEST_SUBMITTED,
            "ACCESS_REQUEST",
            requestId
        );
    }

    @Override
    @Transactional
    public void notifyAccessRequestApproved(Long requestId, Long requesterId, Long approverId) {
        createNotification(
            requesterId,
            "Access Request Approved",
            "Your access request has been approved",
            NotificationType.ACCESS_REQUEST_APPROVED,
            "ACCESS_REQUEST",
            requestId
        );
    }

    @Override
    @Transactional
    public void notifyAccessRequestRejected(Long requestId, Long requesterId, Long approverId) {
        createNotification(
            requesterId,
            "Access Request Rejected",
            "Your access request has been rejected",
            NotificationType.ACCESS_REQUEST_REJECTED,
            "ACCESS_REQUEST",
            requestId
        );
    }

    private User getUserReference(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private NotificationDTO mapToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setRead(notification.isRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReferenceType(notification.getReferenceType());
        dto.setReferenceId(notification.getReferenceId());
        
        // Calculate time ago
        dto.setTimeAgo(calculateTimeAgo(notification.getCreatedAt()));
        
        // Set icon based on notification type
        dto.setIcon(getIconForNotificationType(notification.getType()));
        
        return dto;
    }

    private String calculateTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        
        if (minutes < 60) {
            return minutes + " minutes ago";
        }
        
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return hours + " hours ago";
        }
        
        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 30) {
            return days + " days ago";
        }
        
        long months = ChronoUnit.MONTHS.between(dateTime, now);
        return months + " months ago";
    }

    private String getIconForNotificationType(NotificationType type) {
        return switch (type) {
            case ACCESS_REQUEST_SUBMITTED -> "request_new";
            case ACCESS_REQUEST_APPROVED -> "check_circle";
            case ACCESS_REQUEST_REJECTED -> "cancel";
            case ACCESS_REQUEST_CANCELLED -> "remove_circle";
            case RESOURCE_CREATED -> "add_box";
            case RESOURCE_UPDATED -> "edit";
            case RESOURCE_DELETED -> "delete";
            case PERMISSION_GRANTED -> "lock_open";
            case PERMISSION_REVOKED -> "lock";
            default -> "notifications";
        };
    }

    /**
     * Cleanup old notifications (older than 30 days and read)
     * Runs at 1 AM every day
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Notification> oldNotifications = notificationRepository.findByReadTrueAndCreatedAtBefore(thirtyDaysAgo);
        notificationRepository.deleteAll(oldNotifications);
    }
} 