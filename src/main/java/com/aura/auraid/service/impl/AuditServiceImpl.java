package com.aura.auraid.service.impl;

import com.aura.auraid.model.AuditLog;
import com.aura.auraid.repository.AuditLogRepository;
import com.aura.auraid.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void logEvent(String action, String username, String entityType, String entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .username(username)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    public void logEvent(String action, String username, String entityType, String entityId, String details, HttpServletRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .username(username)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
} 