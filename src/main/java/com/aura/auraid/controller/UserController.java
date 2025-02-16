package com.aura.auraid.controller;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.dto.ChangePasswordDTO;
import com.aura.auraid.dto.UpdateUserRolesDTO;
import com.aura.auraid.enums.UserStatus;
import com.aura.auraid.exception.DuplicateResourceException;
import com.aura.auraid.exception.ResourceNotFoundException;
import com.aura.auraid.model.User;
import com.aura.auraid.repository.UserRepository;
import com.aura.auraid.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

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
    @GetMapping({"/check-username", "/check-username/{username}"})
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(
            @Parameter(description = "Username to check") 
            @PathVariable(required = false) String username,
            @RequestParam(required = false) @Size(min = 3, max = 20) String usernameParam) {
        String usernameToCheck = username != null ? username : usernameParam;
        if (usernameToCheck == null) {
            throw new IllegalArgumentException("Username must be provided either as path variable or request parameter");
        }
        log.debug("Checking username availability for: {}", usernameToCheck);
        boolean exists = userService.existsByUsername(usernameToCheck);
        Map<String, Object> response = new HashMap<>();
        response.put("available", !exists);
        response.put("username", usernameToCheck);
        if (exists) {
            log.debug("Username '{}' is already taken", usernameToCheck);
            response.put("message", "Username is already taken");
        } else {
            log.debug("Username '{}' is available", usernameToCheck);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Check email availability",
        description = "Check if an email is available for registration"
    )
    @GetMapping({"/check-email", "/check-email/{email}"})
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(
            @Parameter(description = "Email to check") 
            @PathVariable(required = false) String email,
            @RequestParam(required = false) @Email @Size(max = 50) String emailParam) {
        String emailToCheck = email != null ? email : emailParam;
        if (emailToCheck == null) {
            throw new IllegalArgumentException("Email must be provided either as path variable or request parameter");
        }
        log.debug("Checking email availability for: {}", emailToCheck);
        boolean exists = userService.existsByEmail(emailToCheck);
        log.debug("Email exists check result: {} for email: {}", exists, emailToCheck);
        Map<String, Object> response = new HashMap<>();
        response.put("available", !exists);
        response.put("email", emailToCheck);
        if (exists) {
            log.debug("Email '{}' is already registered", emailToCheck);
            response.put("message", "Email is already registered");
        } else {
            log.debug("Email '{}' is available", emailToCheck);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Debug user data", description = "Debug endpoint to check user data")
    public ResponseEntity<Map<String, Object>> debugUserData(@PathVariable String username) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("status", user.getStatus());
            userData.put("createdAt", user.getCreatedAt());
            userData.put("updatedAt", user.getUpdatedAt());
            userData.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));
            
            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            log.error("Error fetching debug data for user: {}", username, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCurrentUser(#username)")
    @Operation(
        summary = "Change user password",
        description = "Change password for a user. Requires current password for verification."
    )
    @PostMapping("/{username}/change-password")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Username") @PathVariable String username,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(username, changePasswordDTO);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update user roles",
        description = "Update the roles of a user. The user will always retain the USER role."
    )
    @PatchMapping("/{username}/roles")
    public ResponseEntity<UserDTO> updateUserRoles(
            @Parameter(description = "Username") @PathVariable String username,
            @Valid @RequestBody UpdateUserRolesDTO updateUserRolesDTO) {
        log.debug("Updating roles for user: {}", username);
        UserDTO updatedUser = userService.updateUserRoles(username, updateUserRolesDTO);
        return ResponseEntity.ok(updatedUser);
    }
} 