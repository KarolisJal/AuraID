package com.aura.auraid.service;

import com.aura.auraid.model.AuditLog;
import com.aura.auraid.repository.AuditLogRepository;
import com.aura.auraid.service.impl.AuditServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuditServiceImpl auditService;

    private AuditLog testLog;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        testLog = AuditLog.builder()
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
    void logEvent_BasicLog_Success() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testLog);

        // Act
        auditService.logEvent("LOGIN", "testuser", "USER", "123", "Login successful");

        // Assert
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logEvent_WithRequest_Success() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testLog);

        // Act
        auditService.logEvent("LOGIN", "testuser", "USER", "123", "Login successful", request);

        // Assert
        verify(auditLogRepository).save(argThat(log -> 
            log.getIpAddress().equals("127.0.0.1") &&
            log.getUserAgent().equals("Mozilla/5.0")
        ));
    }

    @Test
    void getRecentLogs_Success() {
        // Arrange
        List<AuditLog> expectedLogs = Arrays.asList(testLog);
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
            .thenReturn(expectedLogs);

        // Act
        List<AuditLog> result = auditService.getRecentLogs(10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testLog, result.get(0));
        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
    }

    @Test
    void getRecentLogsByAction_Success() {
        // Arrange
        List<AuditLog> expectedLogs = Arrays.asList(testLog);
        when(auditLogRepository.findByActionOrderByCreatedAtDesc(eq("LOGIN"), any(PageRequest.class)))
            .thenReturn(expectedLogs);

        // Act
        List<AuditLog> result = auditService.getRecentLogsByAction("LOGIN", 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOGIN", result.get(0).getAction());
    }

    @Test
    void getUserActivity_Success() {
        // Arrange
        List<AuditLog> expectedLogs = Arrays.asList(testLog);
        when(auditLogRepository.findByUsernameOrderByCreatedAtDesc(eq("testuser"), any(PageRequest.class)))
            .thenReturn(expectedLogs);

        // Act
        List<AuditLog> result = auditService.getUserActivity("testuser", 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    void getActivityStatistics_Success() {
        // Arrange
        List<AuditLog> logs = Arrays.asList(
            testLog,
            AuditLog.builder().action("LOGIN").build(),
            AuditLog.builder().action("LOGOUT").build()
        );
        when(auditLogRepository.findByCreatedAtBetween(any(), any())).thenReturn(logs);

        // Act
        Map<String, Long> stats = auditService.getActivityStatistics(now.minusDays(1));

        // Assert
        assertNotNull(stats);
        assertEquals(2, stats.get("LOGIN"));
        assertEquals(1, stats.get("LOGOUT"));
    }

    @Test
    void getFailedLoginAttempts_Success() {
        // Arrange
        AuditLog failedLog = AuditLog.builder()
            .action("LOGIN")
            .details("Login failed")
            .build();
        List<AuditLog> expectedLogs = Arrays.asList(failedLog);
        
        when(auditLogRepository.findByActionAndDetailsContainingOrderByCreatedAtDesc(
            eq("LOGIN"), eq("failed"), any(PageRequest.class)))
            .thenReturn(expectedLogs);

        // Act
        List<AuditLog> result = auditService.getFailedLoginAttempts(10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getDetails().contains("failed"));
    }

    @Test
    void getSuspiciousActivities_Success() {
        // Arrange
        List<AuditLog> logs = Arrays.asList(
            AuditLog.builder()
                .action("LOGIN")
                .details("Login failed")
                .ipAddress("192.168.1.1")
                .username("testuser")
                .createdAt(now)
                .build(),
            AuditLog.builder()
                .action("LOGIN")
                .details("Login failed")
                .ipAddress("192.168.1.1")
                .username("testuser")
                .createdAt(now.plusSeconds(30))  // Multiple failed attempts within a short time window
                .build(),
            AuditLog.builder()
                .action("LOGIN")
                .details("Login failed")
                .ipAddress("192.168.1.1")
                .username("testuser")
                .createdAt(now.plusSeconds(45))  // Multiple failed attempts within a short time window
                .build(),
            AuditLog.builder()
                .action("LOGIN")
                .details("Login failed")
                .ipAddress("192.168.1.1")
                .username("testuser")
                .createdAt(now.plusSeconds(60))  // Multiple failed attempts within a short time window
                .build(),
            AuditLog.builder()
                .action("LOGIN")
                .details("Login failed")
                .ipAddress("192.168.1.1")
                .username("testuser")
                .createdAt(now.plusSeconds(90))  // Multiple failed attempts within a short time window
                .build()
        );
        
        when(auditLogRepository.findByCreatedAtBetween(any(), any())).thenReturn(logs);

        // Act
        List<AuditLog> result = auditService.getSuspiciousActivities(now.minusHours(1));

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(5, result.size(), "Should detect all suspicious login attempts");
        
        // Verify all logs are from the same IP and are failed login attempts
        result.forEach(log -> {
            assertEquals("192.168.1.1", log.getIpAddress());
            assertEquals("LOGIN", log.getAction());
            assertTrue(log.getDetails().contains("failed"));
        });
    }

    @Test
    void getRecentFailedLoginAttempts_Success() {
        // Arrange
        List<AuditLog> failedLogs = Arrays.asList(
            AuditLog.builder()
                .action("LOGIN")
                .details("Login failed")
                .username("testuser")
                .createdAt(now)
                .build(),
            AuditLog.builder()
                .action("LOGIN")
                .details("Login failed")
                .username("testuser")
                .createdAt(now.plusMinutes(1))
                .build()
        );
        
        when(auditLogRepository.findByActionAndDetailsContainingAndCreatedAtBetween(
            eq("LOGIN"), eq("failed"), any(), any()))
            .thenReturn(failedLogs);

        // Act
        Map<String, Integer> result = auditService.getRecentFailedLoginAttempts(now.minusHours(1));

        // Assert
        assertNotNull(result);
        assertEquals(2, result.get("testuser"));
    }

    @Test
    void searchAuditLogs_Success() {
        // Arrange
        List<AuditLog> expectedLogs = Arrays.asList(testLog);
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(expectedLogs);

        // Act
        List<AuditLog> result = auditService.searchAuditLogs(
            "testuser",
            "LOGIN",
            "USER",
            "127.0.0.1",
            now.minusDays(1),
            now,
            10
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        assertEquals("LOGIN", result.get(0).getAction());
        assertEquals("USER", result.get(0).getEntityType());
        assertEquals("127.0.0.1", result.get(0).getIpAddress());
    }

    @Test
    void searchAuditLogs_NoFilters_Success() {
        // Arrange
        List<AuditLog> expectedLogs = Arrays.asList(testLog);
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(expectedLogs);

        // Act
        List<AuditLog> result = auditService.searchAuditLogs(
            null, null, null, null, null, null, 10
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(auditLogRepository).findByCreatedAtBetween(any(), any());
    }
} 