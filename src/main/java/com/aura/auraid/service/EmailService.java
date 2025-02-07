package com.aura.auraid.service;

public interface EmailService {
    void sendVerificationEmail(String to, String verificationToken);
    void sendPasswordResetEmail(String to, String resetToken);
} 