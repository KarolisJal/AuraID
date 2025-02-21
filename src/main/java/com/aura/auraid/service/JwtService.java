package com.aura.auraid.service;

import com.aura.auraid.config.JwtProperties;
import com.aura.auraid.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("authorities", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList());
            
        if (userDetails instanceof CustomUserDetails) {
            extraClaims.put("userId", ((CustomUserDetails) userDetails).getId());
        }
        
        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .setIssuer(jwtProperties.getIssuer())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (token == null || userDetails == null) {
            return false;
        }
        try {
            final String username = extractUsername(token);
            return username != null && 
                   username.equals(userDetails.getUsername()) && 
                   !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        if (token == null) {
            return null;
        }
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration != null && expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    private Claims extractAllClaims(String token) {
        if (token == null) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims == null) {
                return Collections.emptyList();
            }
            
            List<String> authorities = claims.get("authorities", List.class);
            if (authorities == null) {
                return Collections.emptyList();
            }
            
            return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        } catch (JwtException | IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    public Long extractUserId(String token) {
        if (token == null) {
            return null;
        }
        try {
            Claims claims = extractAllClaims(token);
            return claims != null ? claims.get("userId", Long.class) : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
} 