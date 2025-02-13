package com.aura.auraid.service;

import com.aura.auraid.dto.*;
import com.aura.auraid.enums.ERole;
import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.exception.DuplicateResourceException;
import com.aura.auraid.exception.ResourceNotFoundException;
import com.aura.auraid.model.Role;
import com.aura.auraid.model.User;
import com.aura.auraid.model.VerificationToken;
import com.aura.auraid.model.PasswordResetToken;
import com.aura.auraid.repository.RoleRepository;
import com.aura.auraid.repository.UserRepository;
import com.aura.auraid.repository.VerificationTokenRepository;
import com.aura.auraid.repository.PasswordResetTokenRepository;
import com.aura.auraid.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private VerificationTokenRepository tokenRepository;
    
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private AuthRequest authRequest;
    private CreateUserDTO createUserDTO;
    private Role userRole;
    private VerificationToken verificationToken;
    private PasswordResetToken passwordResetToken;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setStatus(UserStatus.ACTIVE);
        Set<Role> roles = new HashSet<>();
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(ERole.USER);
        roles.add(userRole);
        testUser.setRoles(roles);

        // Setup auth request
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        // Setup create user DTO
        createUserDTO = new CreateUserDTO();
        createUserDTO.setUsername("newuser");
        createUserDTO.setEmail("new@example.com");
        createUserDTO.setPassword("password123");
        createUserDTO.setFirstName("New");
        createUserDTO.setLastName("User");

        // Setup verification token
        verificationToken = new VerificationToken();
        verificationToken.setToken("test-token");
        verificationToken.setUser(testUser);
        verificationToken.setExpiryDate(LocalDateTime.now().plusDays(1));
        verificationToken.setUsed(false);

        // Setup password reset token
        passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken("reset-token");
        passwordResetToken.setUser(testUser);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        passwordResetToken.setUsed(false);
    }

    @Test
    void login_Success() {
        // Arrange
        when(userRepository.findByUsername(authRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any())).thenReturn("test-jwt-token");

        // Act
        AuthResponse response = authService.login(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        assertEquals(testUser.getEmail(), response.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_InactiveUser_ThrowsException() {
        // Arrange
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByUsername(authRequest.getUsername())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> authService.login(authRequest));
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(any(ERole.class))).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("test-jwt-token");
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        // Act
        AuthResponse response = authService.register(createUserDTO);

        // Assert
        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        
        // Verify user creation
        verify(userRepository).save(argThat(user -> 
            user.getUsername().equals(createUserDTO.getUsername()) &&
            user.getEmail().equals(createUserDTO.getEmail()) &&
            user.getFirstName().equals(createUserDTO.getFirstName()) &&
            user.getLastName().equals(createUserDTO.getLastName())
        ));
        
        // Verify email verification
        verify(emailService).sendVerificationEmail(
            eq(createUserDTO.getEmail()),
            anyString()
        );
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> authService.register(createUserDTO));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyEmail_Success() {
        // Arrange
        when(tokenRepository.findByToken("test-token")).thenReturn(Optional.of(verificationToken));

        // Act
        VerificationResponse response = authService.verifyEmail("test-token");

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Email verified successfully", response.getMessage());
        assertEquals(UserStatus.ACTIVE, testUser.getStatus());
        assertTrue(verificationToken.isUsed());
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(verificationToken);
    }

    @Test
    void verifyEmail_ExpiredToken_Failure() {
        // Arrange
        verificationToken.setExpiryDate(LocalDateTime.now().minusDays(1));
        when(tokenRepository.findByToken("test-token")).thenReturn(Optional.of(verificationToken));

        // Act
        VerificationResponse response = authService.verifyEmail("test-token");

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Verification token has expired", response.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerificationEmail_Success() {
        // Arrange
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        // Act
        authService.resendVerificationEmail("test@example.com");

        // Assert
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
    }

    @Test
    void resendVerificationEmail_AlreadyVerified_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalStateException.class, 
            () -> authService.resendVerificationEmail("test@example.com"));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void requestPasswordReset_Success() {
        // Arrange
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        authService.requestPasswordReset(request);

        // Assert
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_Success() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setToken("reset-token");
        resetDTO.setNewPassword("newPassword123");
        
        when(passwordResetTokenRepository.findByToken("reset-token"))
            .thenReturn(Optional.of(passwordResetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        // Act
        VerificationResponse response = authService.resetPassword(resetDTO);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Password reset successfully", response.getMessage());
        verify(userRepository).save(testUser);
        verify(passwordResetTokenRepository).save(passwordResetToken);
        assertTrue(passwordResetToken.isUsed());
    }

    @Test
    void resetPassword_ExpiredToken_Failure() {
        // Arrange
        PasswordResetDTO resetDTO = new PasswordResetDTO();
        resetDTO.setToken("reset-token");
        resetDTO.setNewPassword("newPassword123");
        
        passwordResetToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        when(passwordResetTokenRepository.findByToken("reset-token"))
            .thenReturn(Optional.of(passwordResetToken));

        // Act
        VerificationResponse response = authService.resetPassword(resetDTO);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Reset token has expired", response.getMessage());
        verify(userRepository, never()).save(any());
    }
} 