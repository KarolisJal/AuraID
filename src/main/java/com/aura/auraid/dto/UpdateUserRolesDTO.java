package com.aura.auraid.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

@Data
public class UpdateUserRolesDTO {
    @NotEmpty(message = "Roles list cannot be empty")
    private Set<String> roles;
} 