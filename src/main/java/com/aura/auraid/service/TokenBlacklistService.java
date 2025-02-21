package com.aura.auraid.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@Slf4j
public class TokenBlacklistService {
    private final ConcurrentHashMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // Schedule cleanup of expired blacklisted tokens every hour
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }

    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdown();
    }

    public void blacklistToken(String token, long expirationTimeMillis) {
        blacklistedTokens.put(token, expirationTimeMillis);
        log.debug("Token blacklisted until: {}", expirationTimeMillis);
    }

    public boolean isTokenBlacklisted(String token) {
        Long expirationTime = blacklistedTokens.get(token);
        if (expirationTime == null) {
            return false;
        }
        
        // Remove expired token from blacklist
        if (System.currentTimeMillis() > expirationTime) {
            blacklistedTokens.remove(token);
            return false;
        }
        
        return true;
    }

    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        log.debug("Cleaned up expired blacklisted tokens");
    }
} 