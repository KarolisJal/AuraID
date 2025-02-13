package com.aura.auraid.service.impl;

import com.aura.auraid.model.UserActivity;
import com.aura.auraid.service.UserDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserDashboardServiceImpl implements UserDashboardService {

    private final Map<String, List<UserActivity>> mockData = new HashMap<>();

    public UserDashboardServiceImpl() {
        // Initialize with some test data
        UserActivity activity = new UserActivity();
        activity.setUsername("testuser");
        activity.setLastLogin(LocalDateTime.now());
        activity.setLoginCount(5);
        activity.setFailedLoginAttempts(1);
        mockData.put("testuser", Arrays.asList(activity));
    }

    @Override
    public Map<String, Object> getDashboardData(String username) {
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("recentActivity", mockData.getOrDefault(username, new ArrayList<>()));
        dashboardData.put("totalUsers", 10L);
        return dashboardData;
    }

    @Override
    public List<UserActivity> getRecentActivity(int limit) {
        return mockData.values().stream()
                .flatMap(List::stream)
                .limit(limit)
                .toList();
    }

    @Override
    public Map<String, Object> getUserStatistics(String username, LocalDateTime since) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogins", 50L);
        stats.put("averageLoginTime", "10:00 AM");
        return stats;
    }

    @Override
    public List<UserActivity> searchUserActivity(String username, LocalDateTime startDate, LocalDateTime endDate, int limit) {
        return mockData.getOrDefault(username, new ArrayList<>());
    }
} 