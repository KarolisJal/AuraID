package com.aura.auraid.service;

import com.aura.auraid.service.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.mail.MailSendException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_TOKEN = "test-token-123";
    private static final String FRONTEND_URL = "http://localhost:3000";
    private static final String FROM_EMAIL = "noreply@auraid.com";

    @BeforeEach
    void setUp() {
        // Set required properties using ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "frontendUrl", FRONTEND_URL);
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        
        // Setup common mocks
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Email Content</html>");
    }

    @Test
    void sendVerificationEmail_Success() {
        // Arrange
        String expectedVerificationLink = FRONTEND_URL + "/verify?token=" + TEST_TOKEN;

        // Act
        emailService.sendVerificationEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        verify(templateEngine).process(eq("verification-email"), argThat(context -> 
            expectedVerificationLink.equals(context.getVariable("verificationLink"))
        ));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_Success() {
        // Arrange
        String expectedResetLink = FRONTEND_URL + "/reset-password?token=" + TEST_TOKEN;

        // Act
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_TOKEN);

        // Assert
        verify(templateEngine).process(eq("reset-password-email"), argThat(context -> 
            expectedResetLink.equals(context.getVariable("resetLink"))
        ));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_HandlesMessagingException() {
        // Arrange
        doThrow(new MailSendException("Failed to send email"))
            .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_TOKEN);
        }, "Should handle the exception gracefully");

        // Verify that send was attempted
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_HandlesMessagingException() {
        // Arrange
        doThrow(new MailSendException("Failed to send email"))
            .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_TOKEN);
        }, "Should handle the exception gracefully");

        // Verify that send was attempted
        verify(mailSender).send(any(MimeMessage.class));
    }
} 