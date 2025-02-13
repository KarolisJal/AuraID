package com.aura.auraid.controller;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.exception.DuplicateResourceException;
import com.aura.auraid.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create new user",
        description = "Create a new user with the provided details"
    )
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserDTO createUserDTO) {
        // Check if username or email already exists
        if (userService.existsByUsername(createUserDTO.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + createUserDTO.getUsername());
        }
        if (userService.existsByEmail(createUserDTO.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + createUserDTO.getEmail());
        }

        UserDTO createdUser = userService.createUser(createUserDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a user by their ID"
    )
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCurrentUser(#username)")
    @Operation(
        summary = "Get user by username",
        description = "Retrieve a user by their username"
    )
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(
            @Parameter(description = "Username") @PathVariable String username) {
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get all users",
        description = "Retrieve all users with optional status filter"
    )
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @Parameter(description = "Filter users by status") 
            @RequestParam(required = false) UserStatus status) {
        List<UserDTO> users = status != null ? 
            userService.getUsersByStatus(status) : 
            userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Retrieve the currently authenticated user's information"
    )
    @ApiResponse(responseCode = "200", description = "Current user details retrieved successfully")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserDTO user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCurrentUser(#username)")
    @Operation(
        summary = "Update user",
        description = "Update an existing user's information"
    )
    @PutMapping("/{username}")
    public ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "Username") @PathVariable String username,
            @Valid @RequestBody UpdateUserDTO updateUserDTO) {
        // Check if username or email already exists if they are being updated
        if (updateUserDTO.getUsername() != null && 
            userService.existsByUsername(updateUserDTO.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + updateUserDTO.getUsername());
        }
        if (updateUserDTO.getEmail() != null && 
            userService.existsByEmail(updateUserDTO.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + updateUserDTO.getEmail());
        }

        UserDTO updatedUser = userService.updateUser(username, updateUserDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete user",
        description = "Delete a user by their username"
    )
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Username") @PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update user status",
        description = "Update a user's status (ACTIVE, INACTIVE, BLOCKED)"
    )
    @PatchMapping("/{username}/status")
    public ResponseEntity<Void> updateUserStatus(
            @Parameter(description = "Username") @PathVariable String username,
            @Parameter(description = "New user status") @RequestParam UserStatus status) {
        userService.updateUserStatus(username, status);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Check username availability",
        description = "Check if a username is available for registration"
    )
    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsernameAvailability(
            @Parameter(description = "Username to check") @RequestParam String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(!exists);
    }

    @Operation(
        summary = "Check email availability",
        description = "Check if an email is available for registration"
    )
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailAvailability(
            @Parameter(description = "Email to check") @RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(!exists);
    }
} 