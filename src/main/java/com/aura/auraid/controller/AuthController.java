package com.aura.auraid.controller;

import com.aura.auraid.dto.AuthRequest;
import com.aura.auraid.dto.AuthResponse;
import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.VerificationResponse;
import com.aura.auraid.dto.PasswordResetRequestDTO;
import com.aura.auraid.dto.PasswordResetDTO;
import com.aura.auraid.dto.VerificationRequest;
import com.aura.auraid.service.AuthService;
import com.aura.auraid.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Operation(
        summary = "Login user",
        description = "Authenticate a user and return a JWT token"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully authenticated"
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CreateUserDTO request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Request password reset")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset password")
    @PostMapping("/reset-password")
    public ResponseEntity<VerificationResponse> resetPassword(
            @Valid @RequestBody PasswordResetDTO request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @Operation(
        summary = "Test email integration",
        description = "Send a test email to verify SendGrid integration"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Test email sent successfully"
    )
    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail(@RequestParam String email) {
        try {
            emailService.sendVerificationEmail(email, "test-token-123");
            return ResponseEntity.ok("Test email sent successfully to: " + email);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Failed to send email: " + e.getMessage());
        }
    }

    @Operation(summary = "Verify email address")
    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verifyEmail(@Valid @RequestBody VerificationRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request.getToken()));
    }

    @GetMapping("/generate-hash/{password}")
    @Operation(summary = "Generate password hash", description = "Temporary endpoint to generate BCrypt hash")
    public String generateHash(@PathVariable String password) {
        return passwordEncoder.encode(password);
    }
} 