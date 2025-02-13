package com.aura.auraid.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PasswordResetRequestDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;
} 