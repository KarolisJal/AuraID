package com.aura.auraid.service.impl;

import com.aura.auraid.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendVerificationEmail(String to, String verificationToken) {
        Context context = new Context();
        context.setVariable("verificationLink", 
            "http://localhost:8080/api/v1/auth/verify?token=" + verificationToken);

        String emailContent = templateEngine.process("verification-email", context);
        sendHtmlEmail(to, "Verify your email address", emailContent);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        Context context = new Context();
        context.setVariable("resetLink", 
            "http://localhost:8080/api/v1/auth/reset-password?token=" + resetToken);

        String emailContent = templateEngine.process("reset-password-email", context);
        sendHtmlEmail(to, "Reset your password", emailContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
} 