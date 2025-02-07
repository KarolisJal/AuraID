package com.aura.auraid.service;

import com.aura.auraid.dto.AuthRequest;
import com.aura.auraid.dto.AuthResponse;
import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.VerificationResponse;
import com.aura.auraid.dto.PasswordResetRequestDTO;
import com.aura.auraid.dto.PasswordResetDTO;

public interface AuthService {
    AuthResponse login(AuthRequest request);
    AuthResponse register(CreateUserDTO request);
    VerificationResponse verifyEmail(String token);
    void resendVerificationEmail(String email);
    String generateVerificationToken(Long userId);
    void requestPasswordReset(PasswordResetRequestDTO request);
    VerificationResponse resetPassword(PasswordResetDTO request);
} 