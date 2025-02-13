package com.aura.auraid.service;

import com.aura.auraid.model.UserActivity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserDashboardService {
    Map<String, Object> getDashboardData(String username);
    List<UserActivity> getRecentActivity(int limit);
    Map<String, Object> getUserStatistics(String username, LocalDateTime since);
    List<UserActivity> searchUserActivity(String username, LocalDateTime startDate, LocalDateTime endDate, int limit);
} 