package com.aura.auraid.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UpdateUserDTO {
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @Email(message = "Email should be valid")
    @Size(max = 50, message = "Email must not exceed 50 characters")
    private String email;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;
} 