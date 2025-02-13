package com.aura.auraid.controller;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.exception.DuplicateResourceException;
import com.aura.auraid.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDTO userDTO;
    private CreateUserDTO createUserDTO;
    private UpdateUserDTO updateUserDTO;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Setup test UserDTO
        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setCountry("US");
        userDTO.setStatus(UserStatus.ACTIVE);
        userDTO.setCreatedAt(LocalDateTime.now());
        userDTO.setUpdatedAt(LocalDateTime.now());
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        userDTO.setRoles(roles);

        // Setup test CreateUserDTO
        createUserDTO = new CreateUserDTO();
        createUserDTO.setUsername("newuser");
        createUserDTO.setEmail("new@example.com");
        createUserDTO.setPassword("password123");
        createUserDTO.setFirstName("New");
        createUserDTO.setLastName("User");

        // Setup test UpdateUserDTO
        updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setEmail("updated@example.com");
        updateUserDTO.setFirstName("Updated");
        updateUserDTO.setLastName("User");
        updateUserDTO.setCountry("UK");

        // Setup UserDetails for authentication tests
        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void createUser_Success() {
        // Arrange
        when(userService.existsByUsername(anyString())).thenReturn(false);
        when(userService.existsByEmail(anyString())).thenReturn(false);
        when(userService.createUser(any(CreateUserDTO.class))).thenReturn(userDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.createUser(createUserDTO);

        // Assert
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService).createUser(createUserDTO);
    }

    @Test
    void createUser_DuplicateUsername() {
        // Arrange
        when(userService.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> 
            userController.createUser(createUserDTO));
        verify(userService, never()).createUser(any());
    }

    @Test
    void getUserById_Success() {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(userDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.getUserById(1L);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(userDTO, response.getBody());
    }

    @Test
    void getAllUsers_Success() {
        // Arrange
        List<UserDTO> users = Arrays.asList(userDTO);
        when(userService.getAllUsers()).thenReturn(users);

        // Act
        ResponseEntity<List<UserDTO>> response = userController.getAllUsers(null);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(users, response.getBody());
    }

    @Test
    void getAllUsers_WithStatusFilter() {
        // Arrange
        List<UserDTO> activeUsers = Arrays.asList(userDTO);
        when(userService.getUsersByStatus(UserStatus.ACTIVE)).thenReturn(activeUsers);

        // Act
        ResponseEntity<List<UserDTO>> response = userController.getAllUsers(UserStatus.ACTIVE);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(activeUsers, response.getBody());
    }

    @Test
    void getCurrentUser_Success() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(userDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.getCurrentUser(userDetails);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(userDTO, response.getBody());
    }

    @Test
    void updateUser_Success() {
        // Arrange
        when(userService.updateUser(anyString(), any(UpdateUserDTO.class))).thenReturn(userDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.updateUser("testuser", updateUserDTO);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(userDTO, response.getBody());
        verify(userService).updateUser("testuser", updateUserDTO);
    }

    @Test
    void deleteUser_Success() {
        // Act
        ResponseEntity<Void> response = userController.deleteUser("testuser");

        // Assert
        assertEquals(204, response.getStatusCode().value());
        verify(userService).deleteUser("testuser");
    }

    @Test
    void checkUsernameAvailability_Available() {
        // Arrange
        when(userService.existsByUsername("newuser")).thenReturn(false);

        // Act
        ResponseEntity<Boolean> response = userController.checkUsernameAvailability("newuser");

        // Assert
        assertTrue(response.getBody());
    }

    @Test
    void checkEmailAvailability_NotAvailable() {
        // Arrange
        when(userService.existsByEmail("test@example.com")).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = userController.checkEmailAvailability("test@example.com");

        // Assert
        assertFalse(response.getBody());
    }
} 