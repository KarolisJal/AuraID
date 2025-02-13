package com.aura.auraid.service.impl;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.exception.ResourceNotFoundException;
import com.aura.auraid.mapper.UserMapper;
import com.aura.auraid.model.User;
import com.aura.auraid.repository.UserRepository;
import com.aura.auraid.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Comparator;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.aura.auraid.model.AuditLog;
import com.aura.auraid.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public UserDTO createUser(CreateUserDTO createUserDTO) {
        User user = new User();
        user.setFirstName(createUserDTO.getFirstName());
        user.setLastName(createUserDTO.getLastName());
        user.setUsername(createUserDTO.getUsername());
        user.setEmail(createUserDTO.getEmail());
        user.setPassword(passwordEncoder.encode(createUserDTO.getPassword()));
        user.setCountry(createUserDTO.getCountry());
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        return userMapper.toDTO(savedUser);
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDTO(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return userMapper.toDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toDTOList(users);
    }

    @Override
    public List<UserDTO> getUsersByStatus(UserStatus status) {
        List<User> users = userRepository.findByStatus(status);
        return userMapper.toDTOList(users);
    }

    @Override
    @Transactional
    public UserDTO updateUser(String username, UpdateUserDTO updateUserDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (updateUserDTO.getFirstName() != null) {
            user.setFirstName(updateUserDTO.getFirstName());
        }
        if (updateUserDTO.getLastName() != null) {
            user.setLastName(updateUserDTO.getLastName());
        }
        if (updateUserDTO.getEmail() != null) {
            user.setEmail(updateUserDTO.getEmail());
        }
        if (updateUserDTO.getCountry() != null) {
            user.setCountry(updateUserDTO.getCountry());
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void updateUserStatus(String username, UserStatus status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public long getTotalUsers() {
        return userRepository.count();
    }

    @Override
    public long getActiveUsers() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }

    @Override
    public long getNewUsersCount(LocalDateTime since) {
        return userRepository.findByCreatedAtBetween(since, LocalDateTime.now()).size();
    }

    @Override
    public List<UserDTO> getUsersByCreationDateRange(LocalDateTime start, LocalDateTime end) {
        List<User> users = userRepository.findByCreatedAtBetween(start, end);
        return userMapper.toDTOList(users);
    }

    @Override
    public LocalDateTime getLastLoginTime(String username) {
        return userRepository.findByUsername(username)
            .map(User::getLastLoginAt)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public LocalDateTime getLastPasswordChangeTime(String username) {
        return userRepository.findByUsername(username)
            .map(User::getPasswordChangedAt)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public List<Map<String, Object>> getActiveDevices(String username) {
        // This would typically come from a session/device tracking table
        // For now, return a simulated list based on recent audit logs
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<AuditLog> recentLogs = auditLogRepository.findByUsernameAndCreatedAtBetween(
            username, thirtyDaysAgo, LocalDateTime.now());

        return recentLogs.stream()
            .filter(log -> log.getIpAddress() != null && log.getUserAgent() != null)
            .collect(Collectors.groupingBy(
                log -> log.getIpAddress() + "|" + log.getUserAgent(),
                Collectors.collectingAndThen(
                    Collectors.maxBy(Comparator.comparing(AuditLog::getCreatedAt)),
                    optionalLog -> optionalLog.map(log -> {
                        Map<String, Object> device = new HashMap<>();
                        device.put("ipAddress", log.getIpAddress());
                        device.put("userAgent", log.getUserAgent());
                        device.put("lastActivity", log.getCreatedAt());
                        return device;
                    }).orElse(null)
                )
            ))
            .values()
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getActiveSessionsForUser(String username) {
        return getActiveDevices(username); // For now, same as active devices
    }

    @Override
    public List<Map<String, Object>> getRecentSuspiciousActivities(String username) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<AuditLog> suspiciousLogs = auditLogRepository.findByUsernameAndCreatedAtBetween(
            username, twentyFourHoursAgo, LocalDateTime.now());

        return suspiciousLogs.stream()
            .filter(this::isSuspiciousActivity)
            .map(log -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("timestamp", log.getCreatedAt());
                activity.put("action", log.getAction());
                activity.put("ipAddress", log.getIpAddress());
                activity.put("userAgent", log.getUserAgent());
                activity.put("details", log.getDetails());
                return activity;
            })
            .collect(Collectors.toList());
    }

    @Override
    public boolean isMfaEnabled(String username) {
        return userRepository.findByUsername(username)
            .map(User::isMfaEnabled)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public String getPasswordStrength(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // This would typically use a password strength evaluator
        // For now, return a simple evaluation based on password length
        int length = user.getPassword().length();
        if (length >= 12) return "STRONG";
        if (length >= 8) return "MODERATE";
        return "WEAK";
    }

    private boolean isSuspiciousActivity(AuditLog log) {
        if (log.getDetails() != null && log.getDetails().contains("failed")) {
            return true;
        }

        // Check for unusual IP addresses or user agents
        if (log.getIpAddress() != null && isUnusualIpAddress(log.getIpAddress())) {
            return true;
        }

        // Check for rapid succession of actions
        if (hasRapidSuccessionActions(log)) {
            return true;
        }

        return false;
    }

    private boolean isUnusualIpAddress(String ipAddress) {
        // This would typically check against a list of known good IPs
        // or use geolocation to detect unusual locations
        return false;
    }

    private boolean hasRapidSuccessionActions(AuditLog log) {
        LocalDateTime fiveMinutesAgo = log.getCreatedAt().minusMinutes(5);
        List<AuditLog> recentLogs = auditLogRepository.findByUsernameAndCreatedAtBetween(
            log.getUsername(), fiveMinutesAgo, log.getCreatedAt());
        
        // Consider it suspicious if there are more than 20 actions in 5 minutes
        return recentLogs.size() > 20;
    }

    @Override
    public Map<String, Long> getCountryDistribution() {
        List<User> users = userRepository.findAll();
        return users.stream()
            .filter(user -> user.getCountry() != null)
            .collect(Collectors.groupingBy(
                User::getCountry,
                Collectors.counting()
            ));
    }
} 