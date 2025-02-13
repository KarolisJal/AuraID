package com.aura.auraid.controller;

import com.aura.auraid.dto.*;
import com.aura.auraid.service.AuditService;
import com.aura.auraid.service.UserService;
import com.aura.auraid.metrics.CustomMetrics;
import com.aura.auraid.model.AuditLog;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private CustomMetrics customMetrics;

    @Mock
    private Counter mockCounter;

    @InjectMocks
    private DashboardController dashboardController;

    private LocalDateTime now;
    private AuditLog sampleAuditLog;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        // Setup sample AuditLog using builder
        sampleAuditLog = AuditLog.builder()
            .id(1L)
            .action("LOGIN")
            .username("testuser")
            .entityType("USER")
            .entityId("123")
            .details("Login successful")
            .ipAddress("127.0.0.1")
            .userAgent("Mozilla/5.0")
            .createdAt(now)
            .build();
    }

    @Test
    void getStats_ShouldReturnDashboardStats() {
        // Arrange
        when(userService.getTotalUsers()).thenReturn(100L);
        when(userService.getActiveUsers()).thenReturn(50L);
        when(userService.getNewUsersCount(any())).thenReturn(10L);
        
        // Setup metrics for this test
        when(customMetrics.getLoginAttempts()).thenReturn(mockCounter);
        when(customMetrics.getLoginSuccess()).thenReturn(mockCounter);
        when(customMetrics.getLoginFailure()).thenReturn(mockCounter);
        when(customMetrics.getRegistrationAttempts()).thenReturn(mockCounter);
        when(customMetrics.getRegistrationSuccess()).thenReturn(mockCounter);
        when(customMetrics.getRegistrationFailure()).thenReturn(mockCounter);
        when(mockCounter.count()).thenReturn(10.0);

        // Act
        ResponseEntity<DashboardStatsDTO> response = dashboardController.getStats();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        DashboardStatsDTO stats = response.getBody();
        assertNotNull(stats);
        assertEquals(100L, stats.getTotalUsers());
        assertEquals(50L, stats.getActiveUsers());
        assertNotNull(stats.getSecurityMetrics());
        assertNotNull(stats.getUserMetrics());
        assertNotNull(stats.getSystemHealth());
    }

    @Test
    void getSecurityMetrics_ShouldReturnMetrics() {
        // Arrange
        when(customMetrics.getLoginAttempts()).thenReturn(mockCounter);
        when(customMetrics.getLoginSuccess()).thenReturn(mockCounter);
        when(customMetrics.getLoginFailure()).thenReturn(mockCounter);
        when(customMetrics.getRegistrationAttempts()).thenReturn(mockCounter);
        when(customMetrics.getRegistrationSuccess()).thenReturn(mockCounter);
        when(customMetrics.getRegistrationFailure()).thenReturn(mockCounter);
        when(mockCounter.count()).thenReturn(10.0);

        // Act
        ResponseEntity<SecurityMetricsDTO> response = dashboardController.getSecurityMetricsEndpoint();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        SecurityMetricsDTO metrics = response.getBody();
        assertNotNull(metrics);
        assertEquals(10L, metrics.getLoginAttempts());
    }

    @Test
    void getUserTrends_ShouldReturnTrends() {
        // Arrange
        when(userService.getNewUsersCount(any())).thenReturn(5L);

        // Act
        ResponseEntity<Map<String, Object>> response = dashboardController.getUserTrends();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> trends = response.getBody();
        assertNotNull(trends);
        assertEquals(5L, trends.get("last24Hours"));
    }

    @Test
    void getUserActivity_ShouldReturnUserActivities() {
        // Arrange
        List<AuditLog> mockLogs = Collections.singletonList(sampleAuditLog);
        when(auditService.getUserActivity(anyString(), anyInt())).thenReturn(mockLogs);

        // Act
        ResponseEntity<List<UserActivityDTO>> response = dashboardController.getUserActivity("testuser", 10);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        List<UserActivityDTO> activities = response.getBody();
        assertNotNull(activities);
        assertFalse(activities.isEmpty());
        UserActivityDTO activity = activities.get(0);
        assertEquals("testuser", activity.getUsername());
        assertEquals("LOGIN", activity.getAction());
        assertEquals("SUCCESS", activity.getStatus());
    }

    @Test
    void getSecurityEvents_ShouldReturnEvents() {
        // Arrange
        Map<String, Integer> mockFailedLogins = new HashMap<>();
        mockFailedLogins.put("testuser", 6); // High severity
        when(auditService.getRecentFailedLoginAttempts(any())).thenReturn(mockFailedLogins);
        when(auditService.getSuspiciousActivities(any())).thenReturn(Collections.singletonList(sampleAuditLog));

        // Act
        ResponseEntity<Map<String, Object>> response = dashboardController.getSecurityEvents(24, "HIGH");

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> events = response.getBody();
        assertNotNull(events);
        List<SecurityEventDTO> securityEvents = (List<SecurityEventDTO>) events.get("events");
        assertNotNull(securityEvents);
        assertFalse(securityEvents.isEmpty());
        assertEquals(2, securityEvents.size()); // 1 failed login + 1 suspicious activity
        assertEquals("HIGH", securityEvents.get(0).getSeverity());
    }

    @Test
    void getSecurityAlerts_ShouldReturnAlerts() {
        // Arrange
        Map<String, Integer> mockFailedLogins = new HashMap<>();
        mockFailedLogins.put("testuser", 5); // Threshold for alert
        when(auditService.getRecentFailedLoginAttempts(any())).thenReturn(mockFailedLogins);

        // Act
        ResponseEntity<List<SecurityEventDTO>> response = dashboardController.getActiveSecurityAlerts();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        List<SecurityEventDTO> alerts = response.getBody();
        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());
        SecurityEventDTO alert = alerts.get(0);
        assertEquals("ACCOUNT_LOCKED", alert.getEventType());
        assertEquals("HIGH", alert.getSeverity());
        assertEquals("testuser", alert.getUsername());
    }
} 