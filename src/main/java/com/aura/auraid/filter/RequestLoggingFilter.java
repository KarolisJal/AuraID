package com.aura.auraid.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            // Log request
            logRequest(requestWrapper, requestId);
            
            // Continue with the filter chain
            filterChain.doFilter(requestWrapper, responseWrapper);
            
            // Log response
            logResponse(responseWrapper, requestId, System.currentTimeMillis() - startTime);
        } finally {
            // Copy content to response
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        String queryString = request.getQueryString();
        String path = queryString != null ? 
            request.getRequestURI() + "?" + queryString : 
            request.getRequestURI();

        log.info("Request: [{}] {} {} (Client IP: {})", 
            requestId,
            request.getMethod(),
            path,
            request.getRemoteAddr()
        );
    }

    private void logResponse(ContentCachingResponseWrapper response, 
                           String requestId, 
                           long duration) {
        log.info("Response: [{}] Status: {} (Duration: {}ms)",
            requestId,
            response.getStatus(),
            duration
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/v3/api-docs") || 
               path.contains("/swagger-ui/") ||
               path.contains("/actuator/");
    }
} 