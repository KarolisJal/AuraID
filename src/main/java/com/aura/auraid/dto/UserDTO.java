package com.aura.auraid.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import com.aura.auraid.enums.UserStatus;

@Data
public class UserDTO {
    private Long id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 50, message = "Email must not exceed 50 characters")
    private String email;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;

    private Set<String> roles = new HashSet<>();
    
    private UserStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
} 