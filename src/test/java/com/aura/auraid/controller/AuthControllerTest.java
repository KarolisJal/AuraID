package com.aura.auraid.controller;

import com.aura.auraid.dto.*;
import com.aura.auraid.service.AuthService;
import com.aura.auraid.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    private AuthRequest authRequest;
    private AuthResponse authResponse;
    private CreateUserDTO createUserDTO;
    private PasswordResetRequestDTO passwordResetRequestDTO;
    private PasswordResetDTO passwordResetDTO;
    private VerificationRequest verificationRequest;
    private VerificationResponse verificationResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        authResponse = AuthResponse.builder()
            .token("test-token")
            .type("Bearer")
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .build();

        createUserDTO = new CreateUserDTO(
            "John",             // firstName
            "Doe",             // lastName
            "johndoe",         // username
            "john@example.com", // email
            "password123",      // password
            "USA"              // country
        );

        passwordResetRequestDTO = new PasswordResetRequestDTO();
        passwordResetRequestDTO.setEmail("test@example.com");

        passwordResetDTO = new PasswordResetDTO();
        passwordResetDTO.setToken("token123");
        passwordResetDTO.setNewPassword("newPassword123");

        verificationRequest = new VerificationRequest();
        verificationRequest.setToken("verification-token-123");

        verificationResponse = new VerificationResponse();
        verificationResponse.setSuccess(true);
        verificationResponse.setMessage("Email verified successfully");
    }

    @Test
    void login_ShouldReturnAuthResponse() {
        // Arrange
        when(authService.login(any(AuthRequest.class))).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(authResponse, response.getBody());
        verify(authService).login(authRequest);
    }

    @Test
    void register_ShouldReturnAuthResponse() {
        // Arrange
        when(authService.register(any(CreateUserDTO.class))).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.register(createUserDTO);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(authResponse, response.getBody());
        verify(authService).register(argThat(dto -> 
            dto.getFirstName().equals("John") &&
            dto.getLastName().equals("Doe") &&
            dto.getUsername().equals("johndoe") &&
            dto.getEmail().equals("john@example.com") &&
            dto.getPassword().equals("password123") &&
            dto.getCountry().equals("USA")
        ));
    }

    @Test
    void requestPasswordReset_ShouldReturnOk() {
        // Arrange
        doNothing().when(authService).requestPasswordReset(any(PasswordResetRequestDTO.class));

        // Act
        ResponseEntity<Void> response = authController.requestPasswordReset(passwordResetRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(authService).requestPasswordReset(passwordResetRequestDTO);
    }

    @Test
    void resetPassword_ShouldReturnVerificationResponse() {
        // Arrange
        when(authService.resetPassword(any(PasswordResetDTO.class))).thenReturn(verificationResponse);

        // Act
        ResponseEntity<VerificationResponse> response = authController.resetPassword(passwordResetDTO);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(verificationResponse, response.getBody());
        verify(authService).resetPassword(passwordResetDTO);
    }

    @Test
    void testEmail_ShouldReturnSuccessMessage() {
        // Arrange
        String testEmail = "test@example.com";
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        ResponseEntity<String> response = authController.testEmail(testEmail);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Test email sent successfully"));
        verify(emailService).sendVerificationEmail(eq(testEmail), anyString());
    }

    @Test
    void testEmail_ShouldReturnErrorMessage_WhenExceptionOccurs() {
        // Arrange
        String testEmail = "test@example.com";
        doThrow(new RuntimeException("Email service error"))
            .when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act
        ResponseEntity<String> response = authController.testEmail(testEmail);

        // Assert
        assertNotNull(response);
        assertEquals(500, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Failed to send email"));
        verify(emailService).sendVerificationEmail(eq(testEmail), anyString());
    }

    @Test
    void verifyEmail_ShouldReturnVerificationResponse() {
        // Arrange
        when(authService.verifyEmail(anyString())).thenReturn(verificationResponse);

        // Act
        ResponseEntity<VerificationResponse> response = authController.verifyEmail(verificationRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(verificationResponse, response.getBody());
        verify(authService).verifyEmail(verificationRequest.getToken());
    }
} 