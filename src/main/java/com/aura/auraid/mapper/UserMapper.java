package com.aura.auraid.mapper;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
import com.aura.auraid.dto.UserDTO;
import com.aura.auraid.model.User;

@Component
public class UserMapper {
    
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCountry(user.getCountry());
        
        Set<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());
        dto.setRoles(roles);
        
        return dto;
    }
    
    public List<UserDTO> toDTOList(List<User> users) {
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        
        User user = new User();
        user.setId(dto.getId());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setCountry(dto.getCountry());
        
        return user;
    }
} 