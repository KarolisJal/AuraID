package com.aura.auraid.repository;

import com.aura.auraid.model.User;
import com.aura.auraid.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    List<User> findByCountry(String country);
    
    List<User> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);
    
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(UserStatus status);
} 