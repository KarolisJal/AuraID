package com.aura.auraid.service;

import com.aura.auraid.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION = 86400000; // 24 hours
    private static final String TEST_ISSUER = "auraid";
    private static final String INVALID_TOKEN = "invalid.jwt.token";

    @BeforeEach
    void setUp() {
        // Setup test user details
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        userDetails = User.builder()
            .username(TEST_USERNAME)
            .password("password")
            .authorities(authorities)
            .build();

        // Setup JWT properties with lenient mocking
        lenient().when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
        lenient().when(jwtProperties.getExpiration()).thenReturn(TEST_EXPIRATION);
        lenient().when(jwtProperties.getIssuer()).thenReturn(TEST_ISSUER);
    }

    @Test
    void generateToken_Success() {
        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void generateToken_WithExtraClaims_Success() {
        // Arrange
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("customClaim", "customValue");

        // Act
        String token = jwtService.generateToken(extraClaims, userDetails);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        // Arrange
        when(jwtProperties.getExpiration()).thenReturn(-10000L); // Set expiration to 10 seconds in the past
        String token = jwtService.generateToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertFalse(isValid, "Token should be invalid because it's expired");
    }

    @Test
    void isTokenValid_WrongUser_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateToken(userDetails);
        UserDetails wrongUser = User.builder()
            .username("wronguser")
            .password("password")
            .authorities(new ArrayList<>())
            .build();

        // Act
        boolean isValid = jwtService.isTokenValid(token, wrongUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_InvalidToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtService.isTokenValid(INVALID_TOKEN, userDetails);

        // Assert
        assertFalse(isValid, "Invalid token should return false");
    }

    @Test
    void isTokenValid_NullToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtService.isTokenValid(null, userDetails);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_NullUserDetails_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void extractAuthorities_Success() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(token);

        // Assert
        assertNotNull(authorities);
        assertFalse(authorities.isEmpty());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void extractAuthorities_InvalidToken_ReturnsEmptyList() {
        // Act
        Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(INVALID_TOKEN);

        // Assert
        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void generateToken_WithCustomExpiration() {
        // Arrange
        long customExpiration = 3600000; // 1 hour
        when(jwtProperties.getExpiration()).thenReturn(customExpiration);

        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void generateToken_WithMultipleAuthorities() {
        // Arrange
        Set<SimpleGrantedAuthority> multipleAuthorities = new HashSet<>();
        multipleAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        multipleAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        UserDetails userWithMultipleRoles = User.builder()
            .username(TEST_USERNAME)
            .password("password")
            .authorities(multipleAuthorities)
            .build();

        // Act
        String token = jwtService.generateToken(userWithMultipleRoles);
        Collection<? extends GrantedAuthority> extractedAuthorities = jwtService.extractAuthorities(token);

        // Assert
        assertNotNull(token);
        assertEquals(2, extractedAuthorities.size());
        assertTrue(extractedAuthorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(extractedAuthorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void generateToken_WithEmptyAuthorities() {
        // Arrange
        UserDetails userWithNoRoles = User.builder()
            .username(TEST_USERNAME)
            .password("password")
            .authorities(new ArrayList<>())
            .build();

        // Act
        String token = jwtService.generateToken(userWithNoRoles);
        Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(token);

        // Assert
        assertNotNull(token);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void extractUsername_FromInvalidToken_ReturnsNull() {
        // Act
        String username = jwtService.extractUsername(INVALID_TOKEN);

        // Assert
        assertNull(username);
    }

    @Test
    void extractUsername_FromValidToken_Success() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertEquals(TEST_USERNAME, username);
    }

    @Test
    void extractUsername_FromNullToken_ReturnsNull() {
        // Act
        String username = jwtService.extractUsername(null);

        // Assert
        assertNull(username);
    }

    @Test
    void extractAuthorities_NullToken_ReturnsEmptyList() {
        // Act
        Collection<? extends GrantedAuthority> authorities = jwtService.extractAuthorities(null);

        // Assert
        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }
} 