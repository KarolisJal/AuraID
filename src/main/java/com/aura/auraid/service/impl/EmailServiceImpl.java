package com.aura.auraid.service.impl;

import com.aura.auraid.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${app.frontend-url}")
    private String frontendUrl;
    
    @Value("${spring.mail.from}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        Context context = new Context();
        context.setVariable("verificationLink", 
            frontendUrl + "/verify?token=" + verificationToken);

        String emailContent = templateEngine.process("verification-email", context);
        sendHtmlEmail(to, "Verify your email address", emailContent);
        log.info("Verification email sent to: {}", to);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        Context context = new Context();
        context.setVariable("resetLink", 
            frontendUrl + "/reset-password?token=" + resetToken);

        String emailContent = templateEngine.process("reset-password-email", context);
        sendHtmlEmail(to, "Reset your password", emailContent);
        log.info("Password reset email sent to: {}", to);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom("iam.auraid@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
} 