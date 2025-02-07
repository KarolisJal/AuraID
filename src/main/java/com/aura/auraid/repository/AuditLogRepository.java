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
} 