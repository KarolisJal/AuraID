package com.aura.auraid.security;

import com.aura.auraid.service.JwtService;
import com.aura.auraid.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        
        // Skip JWT processing for public endpoints
        if (isPublicEndpoint(requestURI)) {
            log.debug("Skipping JWT authentication for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid Authorization header found for protected endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        
        // Check if token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
            log.warn("Attempt to use blacklisted token");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            username = jwtService.extractUsername(jwt);
            Long userId = jwtService.extractUserId(jwt);
            log.debug("Processing request for username: {}, userId: {}, URI: {}", username, userId, requestURI);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(jwt);
                    log.debug("Token is valid. User: {}, Authorities: {}", username, authorities);
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    // Set userId in request attributes
                    if (userId != null) {
                        request.setAttribute("userId", userId);
                        log.debug("Set userId {} in request attributes", userId);
                    }
                } else {
                    log.warn("Token validation failed for user: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Error processing JWT token", e);
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.contains("/auth/") || 
               uri.contains("/check-username") || 
               uri.contains("/check-email") || 
               uri.contains("/swagger-ui") || 
               uri.contains("/v3/api-docs") ||
               uri.contains("/actuator/health");
    }
} 