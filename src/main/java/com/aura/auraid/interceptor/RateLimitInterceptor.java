package com.aura.auraid.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    private static final int DEFAULT_LIMIT = 100; // 100 requests per minute
    private static final int CHECK_ENDPOINT_LIMIT = 50; // 50 requests per minute for check endpoints
    private static final int DASHBOARD_LIMIT = 200; // 200 requests per minute for dashboard endpoints
    private static final int AUDIT_ENDPOINT_LIMIT = 300; // 300 requests per minute for audit endpoints

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();
        
        // Create a composite key that includes both IP and endpoint type
        String cacheKey = getCacheKey(clientIp, requestUri);
        int limit = getLimit(requestUri);
        
        AtomicInteger count = requestCounts.get(cacheKey, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        
        return true;
    }

    private String getCacheKey(String clientIp, String requestUri) {
        String endpointType = getEndpointType(requestUri);
        return clientIp + ":" + endpointType;
    }

    private String getEndpointType(String uri) {
        if (uri.contains("/check-")) return "check";
        if (uri.contains("/dashboard")) return "dashboard";
        if (uri.contains("/audit")) return "audit";
        return "default";
    }

    private int getLimit(String uri) {
        if (uri.contains("/check-")) return CHECK_ENDPOINT_LIMIT;
        if (uri.contains("/dashboard")) return DASHBOARD_LIMIT;
        if (uri.contains("/audit")) return AUDIT_ENDPOINT_LIMIT;
        return DEFAULT_LIMIT;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
} 