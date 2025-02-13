package com.aura.auraid.repository;

import com.aura.auraid.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsername(String username);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    List<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    List<AuditLog> findByActionAndDetailsContainingOrderByCreatedAtDesc(
        String action, String details, Pageable pageable);
    List<AuditLog> findByActionAndDetailsContainingAndCreatedAtBetween(
        String action, String details, LocalDateTime start, LocalDateTime end);
    
    // New methods for enhanced audit functionality
    List<AuditLog> findByUsernameAndCreatedAtBetween(
        String username, LocalDateTime start, LocalDateTime end);
    
    List<AuditLog> findByIpAddressAndCreatedAtBetween(
        String ipAddress, LocalDateTime start, LocalDateTime end);
    
    List<AuditLog> findByEntityTypeAndCreatedAtBetween(
        String entityType, LocalDateTime start, LocalDateTime end);
    
    List<AuditLog> findByUsernameAndActionAndCreatedAtBetween(
        String username, String action, LocalDateTime start, LocalDateTime end);
} 