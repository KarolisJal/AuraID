package com.aura.auraid.security;

import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.model.Role;
import com.aura.auraid.model.User;
import com.aura.auraid.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setStatus(UserStatus.ACTIVE);
        
        Set<Role> roles = new HashSet<>();
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(com.aura.auraid.enums.ERole.USER);
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    @Test
    void loadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Assert
        assertNotNull(userDetails);
        assertEquals(TEST_USERNAME, userDetails.getUsername());
        assertEquals(testUser.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.isEnabled(), "Active user should be enabled");
        assertTrue(userDetails.isAccountNonExpired(), "Account should not be expired");
        assertTrue(userDetails.isCredentialsNonExpired(), "Credentials should not be expired");
        assertTrue(userDetails.isAccountNonLocked(), "Account should not be locked");
        
        // Verify authorities
        assertTrue(userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername(TEST_USERNAME))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(TEST_USERNAME));
    }

    @Test
    void loadUserByUsername_InactiveUser_DisabledUser() {
        // Arrange
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Assert
        assertFalse(userDetails.isEnabled(), "Inactive user should be disabled");
    }

    @Test
    void loadUserByUsername_BlockedUser_LockedAccount() {
        // Arrange
        testUser.setStatus(UserStatus.BLOCKED);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Assert
        assertFalse(userDetails.isAccountNonLocked(), "Blocked user's account should be locked");
    }

    @Test
    void loadUserByUsername_MultipleRoles() {
        // Arrange
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(com.aura.auraid.enums.ERole.ADMIN);
        testUser.getRoles().add(adminRole);
        
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Assert
        assertEquals(2, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_ADMIN")));
        assertTrue(userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_NoRoles() {
        // Arrange
        testUser.setRoles(new HashSet<>());
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEST_USERNAME);

        // Assert
        assertTrue(userDetails.getAuthorities().isEmpty());
    }
} 