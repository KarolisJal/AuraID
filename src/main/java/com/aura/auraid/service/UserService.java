package com.aura.auraid.service;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.dto.ChangePasswordDTO;
import com.aura.auraid.dto.UpdateUserRolesDTO;
import com.aura.auraid.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserService {
    UserDTO createUser(CreateUserDTO createUserDTO);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    List<UserDTO> getAllUsers();
    List<UserDTO> getUsersByStatus(UserStatus status);
    List<UserDTO> getUsersByRole(String roleName);
    UserDTO updateUser(String username, UpdateUserDTO updateUserDTO);
    void deleteUser(String username);
    void updateUserStatus(String username, UserStatus status);
    UserDTO updateUserRoles(String username, UpdateUserRolesDTO updateUserRolesDTO);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long getTotalUsers();
    long getActiveUsers();
    long getNewUsersCount(LocalDateTime since);
    List<UserDTO> getUsersByCreationDateRange(LocalDateTime start, LocalDateTime end);
    LocalDateTime getLastLoginTime(String username);
    LocalDateTime getLastPasswordChangeTime(String username);
    List<Map<String, Object>> getActiveDevices(String username);
    List<Map<String, Object>> getActiveSessionsForUser(String username);
    List<Map<String, Object>> getRecentSuspiciousActivities(String username);
    boolean isMfaEnabled(String username);
    String getPasswordStrength(String username);
    Map<String, Long> getCountryDistribution();
    void changePassword(String username, ChangePasswordDTO changePasswordDTO);
} 