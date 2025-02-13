package com.aura.auraid.service;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.exception.ResourceNotFoundException;
import com.aura.auraid.mapper.UserMapper;
import com.aura.auraid.model.User;
import com.aura.auraid.model.AuditLog;
import com.aura.auraid.repository.UserRepository;
import com.aura.auraid.repository.AuditLogRepository;
import com.aura.auraid.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;
    private CreateUserDTO createUserDTO;
    private UpdateUserDTO updateUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("encodedPassword");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCountry("US");

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setFirstName("Test");
        testUserDTO.setLastName("User");
        testUserDTO.setStatus(UserStatus.ACTIVE);
        testUserDTO.setCountry("US");

        createUserDTO = new CreateUserDTO();
        createUserDTO.setUsername("newuser");
        createUserDTO.setEmail("new@example.com");
        createUserDTO.setPassword("password123");
        createUserDTO.setFirstName("New");
        createUserDTO.setLastName("User");
        createUserDTO.setCountry("US");

        updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setFirstName("Updated");
        updateUserDTO.setLastName("Name");
        updateUserDTO.setEmail("updated@example.com");
        updateUserDTO.setCountry("UK");
    }

    @Test
    void createUser_Success() {
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);

        UserDTO result = userService.createUser(createUserDTO);

        assertNotNull(result);
        assertEquals(testUserDTO.getUsername(), result.getUsername());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(createUserDTO.getPassword());
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(testUserDTO.getId(), result.getId());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
        verify(userRepository).findById(99L);
    }

    @Test
    void getUserByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserByUsername("testuser");

        assertNotNull(result);
        assertEquals(testUserDTO.getUsername(), result.getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getAllUsers_Success() {
        List<User> users = Arrays.asList(testUser);
        List<UserDTO> userDTOs = Arrays.asList(testUserDTO);

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toDTOList(users)).thenReturn(userDTOs);

        List<UserDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void updateUser_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);

        UserDTO result = userService.updateUser("testuser", updateUserDTO);

        assertNotNull(result);
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        userService.deleteUser("testuser");

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).delete(testUser);
    }

    @Test
    void updateUserStatus_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateUserStatus("testuser", UserStatus.INACTIVE);

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).save(testUser);
        assertEquals(UserStatus.INACTIVE, testUser.getStatus());
    }

    @Test
    void existsByUsername_True() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertTrue(userService.existsByUsername("testuser"));
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    void existsByEmail_True() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertTrue(userService.existsByEmail("test@example.com"));
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    void getTotalUsers_Success() {
        when(userRepository.count()).thenReturn(5L);

        assertEquals(5L, userService.getTotalUsers());
        verify(userRepository).count();
    }

    @Test
    void getActiveUsers_Success() {
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(3L);

        assertEquals(3L, userService.getActiveUsers());
        verify(userRepository).countByStatus(UserStatus.ACTIVE);
    }

    @Test
    void getNewUsersCount_Success() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<User> newUsers = Arrays.asList(testUser);
        
        when(userRepository.findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(newUsers);

        assertEquals(1L, userService.getNewUsersCount(since));
        verify(userRepository).findByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getUsersByCreationDateRange_Success() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<User> users = Arrays.asList(testUser);
        List<UserDTO> userDTOs = Arrays.asList(testUserDTO);

        when(userRepository.findByCreatedAtBetween(start, end)).thenReturn(users);
        when(userMapper.toDTOList(users)).thenReturn(userDTOs);

        List<UserDTO> result = userService.getUsersByCreationDateRange(start, end);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(userRepository).findByCreatedAtBetween(start, end);
    }

    @Test
    void getLastLoginTime_Success() {
        LocalDateTime lastLogin = LocalDateTime.now();
        testUser.setLastLoginAt(lastLogin);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertEquals(lastLogin, userService.getLastLoginTime("testuser"));
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void isMfaEnabled_Success() {
        testUser.setMfaEnabled(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertTrue(userService.isMfaEnabled("testuser"));
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getPasswordStrength_Success() {
        testUser.setPassword("VeryStrongPassword123!");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        String strength = userService.getPasswordStrength("testuser");
        assertNotNull(strength);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUsersByStatus_Success() {
        List<User> users = Arrays.asList(testUser);
        List<UserDTO> userDTOs = Arrays.asList(testUserDTO);

        when(userRepository.findByStatus(UserStatus.ACTIVE)).thenReturn(users);
        when(userMapper.toDTOList(users)).thenReturn(userDTOs);

        List<UserDTO> result = userService.getUsersByStatus(UserStatus.ACTIVE);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(userRepository).findByStatus(UserStatus.ACTIVE);
    }

    @Test
    void getLastPasswordChangeTime_Success() {
        LocalDateTime passwordChange = LocalDateTime.now();
        testUser.setPasswordChangedAt(passwordChange);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertEquals(passwordChange, userService.getLastPasswordChangeTime("testuser"));
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getActiveDevices_Success() {
        LocalDateTime now = LocalDateTime.now();
        AuditLog log = new AuditLog();
        log.setIpAddress("192.168.1.1");
        log.setUserAgent("Mozilla/5.0");
        log.setCreatedAt(now);
        
        when(auditLogRepository.findByUsernameAndCreatedAtBetween(
            eq("testuser"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(log));

        List<Map<String, Object>> result = userService.getActiveDevices("testuser");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("192.168.1.1", result.get(0).get("ipAddress"));
        assertEquals("Mozilla/5.0", result.get(0).get("userAgent"));
    }

    @Test
    void getRecentSuspiciousActivities_Success() {
        LocalDateTime now = LocalDateTime.now();
        AuditLog log = new AuditLog();
        log.setAction("LOGIN");
        log.setDetails("Login failed");
        log.setIpAddress("192.168.1.1");
        log.setUserAgent("Mozilla/5.0");
        log.setCreatedAt(now);
        
        when(auditLogRepository.findByUsernameAndCreatedAtBetween(
            eq("testuser"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(log));

        List<Map<String, Object>> result = userService.getRecentSuspiciousActivities("testuser");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("LOGIN", result.get(0).get("action"));
        assertEquals("192.168.1.1", result.get(0).get("ipAddress"));
    }

    @Test
    void getUserByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByUsername("nonexistent"));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void updateUser_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser("nonexistent", updateUserDTO));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void deleteUser_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser("nonexistent"));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void updateUserStatus_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> userService.updateUserStatus("nonexistent", UserStatus.INACTIVE));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void getLastLoginTime_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getLastLoginTime("nonexistent"));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void getLastPasswordChangeTime_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, 
            () -> userService.getLastPasswordChangeTime("nonexistent"));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void isMfaEnabled_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.isMfaEnabled("nonexistent"));
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void getPasswordStrength_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getPasswordStrength("nonexistent"));
        verify(userRepository).findByUsername("nonexistent");
    }
} 