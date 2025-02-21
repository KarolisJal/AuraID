package com.aura.auraid.service;

import com.aura.auraid.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a notification to a specific user via WebSocket
     */
    public void sendNotificationToUser(Long userId, NotificationDTO notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );
    }

    /**
     * Send a notification to all users via WebSocket
     */
    public void broadcastNotification(NotificationDTO notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    /**
     * Send unread notification count to a specific user
     */
    public void sendUnreadCountToUser(Long userId, long count) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications/count",
            count
        );
    }
} 