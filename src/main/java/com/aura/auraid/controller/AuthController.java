package com.aura.auraid.controller;

import com.aura.auraid.dto.AuthRequest;
import com.aura.auraid.dto.AuthResponse;
import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.VerificationResponse;
import com.aura.auraid.dto.PasswordResetRequestDTO;
import com.aura.auraid.dto.PasswordResetDTO;
import com.aura.auraid.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

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
} 