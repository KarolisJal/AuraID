package com.aura.auraid.service;

import com.aura.auraid.dto.CreateUserDTO;
import com.aura.auraid.dto.UpdateUserDTO;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.enums.UserStatus;
import java.util.List;

public interface UserService {
    UserDTO createUser(CreateUserDTO createUserDTO);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    List<UserDTO> getAllUsers();
    List<UserDTO> getUsersByStatus(UserStatus status);
    UserDTO updateUser(String username, UpdateUserDTO updateUserDTO);
    void deleteUser(String username);
    void updateUserStatus(String username, UserStatus status);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long getTotalUsers();
    long getActiveUsers();
} 