package com.aura.auraid.service.impl;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.dto.ChangePasswordDTO;
import com.aura.auraid.dto.UpdateUserRolesDTO;
import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.enums.ERole;
import com.aura.auraid.exception.ResourceNotFoundException;
import com.aura.auraid.exception.InvalidCredentialsException;
import com.aura.auraid.mapper.UserMapper;
import com.aura.auraid.model.User;
import com.aura.auraid.model.Role;
import com.aura.auraid.repository.UserRepository;
import com.aura.auraid.repository.RoleRepository;
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
import java.util.Set;
import java.util.HashSet;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.aura.auraid.model.AuditLog;
import com.aura.auraid.repository.AuditLogRepository;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditLogRepository auditLogRepository;
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

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

    @Cacheable(value = "usernameExists", key = "#username", unless = "#result == false")
    public boolean existsByUsername(String username) {
        log.debug("Checking if username exists (case-insensitive): {}", username);
        boolean exists = userRepository.existsByUsernameIgnoreCase(username);
        log.debug("Username '{}' exists: {}", username, exists);
        return exists;
    }

    @Cacheable(value = "emailExists", key = "#email", unless = "#result == false")
    public boolean existsByEmail(String email) {
        log.debug("Checking if email exists (case-insensitive): {}", email);
        boolean exists = userRepository.existsByEmailIgnoreCase(email.toLowerCase());
        log.debug("Email '{}' exists: {}", email, exists);
        return exists;
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
        // Since we no longer track last login time, return current time
        return LocalDateTime.now();
    }

    @Override
    public LocalDateTime getLastPasswordChangeTime(String username) {
        // Since we no longer track password changes, return user's creation time
        return userRepository.findByUsername(username)
            .map(User::getCreatedAt)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Override
    public List<Map<String, Object>> getActiveDevices(String username) {
        // Return a simulated active device (current session)
        Map<String, Object> currentDevice = new HashMap<>();
        currentDevice.put("ipAddress", "127.0.0.1");
        currentDevice.put("userAgent", "Current Session");
        currentDevice.put("lastActivity", LocalDateTime.now());
        return List.of(currentDevice);
    }

    @Override
    public List<Map<String, Object>> getActiveSessionsForUser(String username) {
        return getActiveDevices(username);
    }

    @Override
    public List<Map<String, Object>> getRecentSuspiciousActivities(String username) {
        // Since we're not tracking suspicious activities anymore, return empty list
        return List.of();
    }

    @Override
    public boolean isMfaEnabled(String username) {
        // Since MFA is not supported, always return false
        return false;
    }

    @Override
    public String getPasswordStrength(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        
        // Simple password strength check based on length
        int length = user.getPassword().length();
        if (length >= 12) return "STRONG";
        if (length >= 8) return "MODERATE";
        return "WEAK";
    }

    private boolean isSuspiciousActivity(AuditLog log) {
        // Since we're not tracking suspicious activities, always return false
        return false;
    }

    private boolean isUnusualIpAddress(String ipAddress) {
        return false;
    }

    private boolean hasRapidSuccessionActions(AuditLog log) {
        return false;
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

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordDTO changePasswordDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        // Verify current password
        if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Set new password
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserDTO updateUserRoles(String username, UpdateUserRolesDTO updateUserRolesDTO) {
        log.debug("Updating roles for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : updateUserRolesDTO.getRoles()) {
            try {
                ERole eRole = ERole.valueOf(roleName.toUpperCase());
                Role role = roleRepository.findByName(eRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                newRoles.add(role);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role name: " + roleName);
            }
        }

        // Ensure user has at least the USER role
        Role userRole = roleRepository.findByName(ERole.USER)
            .orElseThrow(() -> new RuntimeException("Default USER role not found"));
        newRoles.add(userRole);

        user.setRoles(newRoles);
        User updatedUser = userRepository.save(user);
        log.debug("Updated roles for user: {}. New roles: {}", username, 
            newRoles.stream().map(r -> r.getName().name()).collect(Collectors.joining(", ")));
        
        return userMapper.toDTO(updatedUser);
    }
} 