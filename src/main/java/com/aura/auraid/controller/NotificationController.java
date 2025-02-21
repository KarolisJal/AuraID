package com.aura.auraid.controller;

import com.aura.auraid.dto.NotificationDTO;
import com.aura.auraid.dto.PageResponseDTO;
import com.aura.auraid.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponseDTO<NotificationDTO>> getUserNotifications(
            @RequestAttribute Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
    }

    @GetMapping("/unread")
    public ResponseEntity<PageResponseDTO<NotificationDTO>> getUnreadNotifications(
            @RequestAttribute Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId, pageable));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(@RequestAttribute Long userId) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(userId));
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@RequestAttribute Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
} 