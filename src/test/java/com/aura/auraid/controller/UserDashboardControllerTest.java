package com.aura.auraid.controller;

import com.aura.auraid.model.UserActivity;
import com.aura.auraid.service.UserDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@EnableWebMvc
class UserDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserDashboardService userDashboardService;

    @InjectMocks
    private UserDashboardController userDashboardController;

    private UserActivity testUserActivity;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userDashboardController)
                .build();

        testDateTime = LocalDateTime.now();
        
        testUserActivity = new UserActivity();
        testUserActivity.setUsername("testuser");
        testUserActivity.setLastLogin(testDateTime);
        testUserActivity.setLoginCount(5);
        testUserActivity.setFailedLoginAttempts(1);
    }

    @Test
    void getUserDashboard_Success() throws Exception {
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("recentActivity", Arrays.asList(testUserActivity));
        dashboardData.put("totalUsers", 10L);
        
        when(userDashboardService.getDashboardData("testuser")).thenReturn(dashboardData);

        mockMvc.perform(get("/api/v1/dashboard/user/testuser")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10));
    }

    @Test
    void getRecentActivity_Success() throws Exception {
        List<UserActivity> activities = Arrays.asList(testUserActivity);
        when(userDashboardService.getRecentActivity(10)).thenReturn(activities);

        mockMvc.perform(get("/api/v1/dashboard/recent-activity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].loginCount").value(5));
    }

    @Test
    void getUserStatistics_Success() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogins", 50L);
        stats.put("averageLoginTime", "10:00 AM");
        
        when(userDashboardService.getUserStatistics(eq("testuser"), any(LocalDateTime.class)))
            .thenReturn(stats);

        mockMvc.perform(get("/api/v1/dashboard/user/testuser/statistics")
                .param("since", testDateTime.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogins").value(50));
    }

    @Test
    void searchUserActivity_Success() throws Exception {
        List<UserActivity> activities = Arrays.asList(testUserActivity);
        
        when(userDashboardService.searchUserActivity(
            eq("testuser"), any(LocalDateTime.class), any(LocalDateTime.class), eq(10)
        )).thenReturn(activities);

        mockMvc.perform(get("/api/v1/dashboard/search")
                .param("username", "testuser")
                .param("startDate", testDateTime.toString())
                .param("endDate", testDateTime.plusDays(1).toString())
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    void searchUserActivity_InvalidLimit() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/search")
                .param("limit", "1001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
} 