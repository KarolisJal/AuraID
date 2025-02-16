package com.aura.auraid.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestId = UUID.randomUUID().toString();
        String uri = httpRequest.getRequestURI();
        
        boolean isAvailabilityCheck = uri.contains("/check-username") || uri.contains("/check-email");
        boolean isPublicEndpoint = isPublicEndpoint(uri);
        
        // Always log availability checks, otherwise only log non-public endpoints
        if (isAvailabilityCheck || !isPublicEndpoint) {
            logRequest(httpRequest, requestId);
        }

        long startTime = System.currentTimeMillis();
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - startTime;

        // Always log availability checks, otherwise only log slow public endpoints
        if (isAvailabilityCheck || !isPublicEndpoint || duration > 200) {
            logResponse(httpResponse, requestId, duration);
        }
    }

    private void logRequest(HttpServletRequest request, String requestId) {
        String queryString = request.getQueryString();
        String path = queryString != null ? 
            request.getRequestURI() + "?" + queryString : 
            request.getRequestURI();
            
        log.debug("Request: [{}] {} {} (Client IP: {})",
                requestId,
                request.getMethod(),
                path,
                request.getRemoteAddr());
    }

    private void logResponse(HttpServletResponse response, String requestId, long duration) {
        log.debug("Response: [{}] Status: {} (Duration: {}ms)",
                requestId,
                response.getStatus(),
                duration);
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.contains("/check-") || 
               uri.contains("/auth/") || 
               uri.contains("/swagger-ui") || 
               uri.contains("/v3/api-docs") ||
               uri.contains("/actuator/health");
    }
} 