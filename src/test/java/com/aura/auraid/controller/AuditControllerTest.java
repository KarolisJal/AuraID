package com.aura.auraid.controller;

import com.aura.auraid.model.AuditLog;
import com.aura.auraid.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    private AuditLog testAuditLog;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditController)
                .build();

        testDateTime = LocalDateTime.now();
        
        testAuditLog = new AuditLog();
        testAuditLog.setId(1L);
        testAuditLog.setUsername("testuser");
        testAuditLog.setAction("LOGIN");
        testAuditLog.setDetails("Test login action");
        testAuditLog.setIpAddress("192.168.1.1");
        testAuditLog.setUserAgent("Mozilla/5.0");
        testAuditLog.setCreatedAt(testDateTime);
    }

    @Test
    void getRecentLogs_Success() throws Exception {
        List<AuditLog> logs = Arrays.asList(testAuditLog);
        when(auditService.getRecentLogs(10)).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit/recent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].action").value("LOGIN"));
    }

    @Test
    void getStatistics_Success() throws Exception {
        Map<String, Long> stats = new HashMap<>();
        stats.put("LOGIN", 10L);
        stats.put("LOGOUT", 5L);
        
        when(auditService.getActivityStatistics(any(LocalDateTime.class))).thenReturn(stats);

        mockMvc.perform(get("/api/v1/audit/statistics")
                .param("since", testDateTime.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.LOGIN").value(10))
                .andExpect(jsonPath("$.LOGOUT").value(5));
    }

    @Test
    void getFailedLogins_Success() throws Exception {
        testAuditLog.setAction("LOGIN_FAILED");
        List<AuditLog> logs = Arrays.asList(testAuditLog);
        
        when(auditService.getFailedLoginAttempts(10)).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit/failed-logins")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("LOGIN_FAILED"));
    }

    @Test
    void getSuspiciousActivities_Success() throws Exception {
        testAuditLog.setAction("SUSPICIOUS_LOGIN");
        List<AuditLog> logs = Arrays.asList(testAuditLog);
        
        when(auditService.getSuspiciousActivities(any(LocalDateTime.class))).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit/suspicious")
                .param("since", testDateTime.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("SUSPICIOUS_LOGIN"));
    }

    @Test
    void getFailedLoginAttempts_Success() throws Exception {
        Map<String, Integer> attempts = new HashMap<>();
        attempts.put("testuser", 3);
        
        when(auditService.getRecentFailedLoginAttempts(any(LocalDateTime.class))).thenReturn(attempts);

        mockMvc.perform(get("/api/v1/audit/failed-login-attempts")
                .param("since", testDateTime.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testuser").value(3));
    }

    @Test
    void searchAuditLogs_Success() throws Exception {
        List<AuditLog> logs = Arrays.asList(testAuditLog);
        
        when(auditService.searchAuditLogs(
            eq("testuser"), eq("LOGIN"), eq("USER"), eq("192.168.1.1"),
            any(LocalDateTime.class), any(LocalDateTime.class), eq(10)
        )).thenReturn(logs);

        mockMvc.perform(get("/api/v1/audit/search")
                .param("username", "testuser")
                .param("action", "LOGIN")
                .param("entityType", "USER")
                .param("ipAddress", "192.168.1.1")
                .param("startDate", testDateTime.toString())
                .param("endDate", testDateTime.plusDays(1).toString())
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    void getUserActivitySummary_Success() throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalActions", 10L);
        summary.put("lastActivity", testDateTime);
        
        when(auditService.getUserActivitySummary(eq("testuser"), any(LocalDateTime.class)))
            .thenReturn(summary);

        mockMvc.perform(get("/api/v1/audit/user/testuser/activity-summary")
                .param("since", testDateTime.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActions").value(10));
    }

    @Test
    void searchAuditLogs_InvalidAction() throws Exception {
        mockMvc.perform(get("/api/v1/audit/search")
                .param("action", "invalid_action")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
} 