package com.aura.auraid.service.impl;

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
import com.aura.auraid.service.AuthService;
import com.aura.auraid.service.EmailService;
import com.aura.auraid.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aura.auraid.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();
                
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalStateException("Please verify your email before logging in");
        }

        var userDetails = createUserDetails(user);
        var token = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(CreateUserDTO request) {
        // Check username and email in a case-insensitive manner
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setCountry(request.getCountry() != null ? request.getCountry().trim() : null);
        user.setStatus(UserStatus.INACTIVE);
        user.setRoles(new HashSet<>()); // Initialize empty roles set
        
        // Save user first to get the ID
        User savedUser = userRepository.save(user);
        userRepository.flush(); // Ensure user is saved before role assignment
        
        Set<Role> roles = new HashSet<>();
        
        // Add USER role first
        Role userRole = roleRepository.findByName(ERole.USER)
            .orElseThrow(() -> new RuntimeException("Default role not found"));
        roles.add(userRole);
        
        // If username is KarolisJal (case-sensitive), add ADMIN role
        if ("KarolisJal".equals(savedUser.getUsername())) {
            Role adminRole = roleRepository.findByName(ERole.ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found"));
            roles.add(adminRole);
            savedUser.setStatus(UserStatus.ACTIVE); // Auto-activate admin user
        }
        
        savedUser.setRoles(roles);
        savedUser = userRepository.save(savedUser);
        userRepository.flush(); // Ensure roles are saved before proceeding

        // Send verification email only for non-admin users
        if (savedUser.getStatus() == UserStatus.INACTIVE) {
            try {
                String verificationToken = generateVerificationToken(savedUser.getId());
                emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
            } catch (Exception e) {
                // Log the error but don't fail the registration
                // The user can request a new verification email later
                log.error("Failed to send verification email to {}: {}", savedUser.getEmail(), e.getMessage());
            }
        }

        var userDetails = createUserDetails(savedUser);
        var token = jwtService.generateToken(userDetails);
        
        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .build();
    }

    @Override
    @Transactional
    public VerificationResponse verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            return VerificationResponse.builder()
                    .success(false)
                    .message("Verification token has expired")
                    .build();
        }

        if (verificationToken.isUsed()) {
            return VerificationResponse.builder()
                    .success(false)
                    .message("Verification token has already been used")
                    .build();
        }

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        return VerificationResponse.builder()
                .success(true)
                .message("Email verified successfully")
                .build();
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalStateException("Email is already verified");
        }

        String verificationToken = generateVerificationToken(user.getId());
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
    }

    @Override
    public String generateVerificationToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        tokenRepository.save(verificationToken);

        return token;
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Invalidate any existing unused reset tokens
        passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getId())
                .ifPresent(token -> {
                    token.setUsed(true);
                    passwordResetTokenRepository.save(token);
                });

        // Generate new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Override
    @Transactional
    public VerificationResponse resetPassword(PasswordResetDTO request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid reset token"));

        if (resetToken.isExpired()) {
            return VerificationResponse.builder()
                    .success(false)
                    .message("Reset token has expired")
                    .build();
        }

        if (resetToken.isUsed()) {
            return VerificationResponse.builder()
                    .success(false)
                    .message("Reset token has already been used")
                    .build();
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return VerificationResponse.builder()
                .success(true)
                .message("Password reset successfully")
                .build();
    }

    private UserDetails createUserDetails(User user) {
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toSet());

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                user.getStatus() != UserStatus.BLOCKED,
                true,
                true,
                user.getStatus() != UserStatus.INACTIVE
        );
    }
} 