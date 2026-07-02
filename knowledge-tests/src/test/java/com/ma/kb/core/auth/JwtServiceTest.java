package com.ma.kb.core.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    // 用 Base64 编码的 256 位密钥
    private static final String SECRET = "dGhpc0lzYVNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHk=";
    private static final long EXPIRES_IN = 7200;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiresIn", EXPIRES_IN);
    }

    @Test
    void generateToken() {
        String token = jwtService.generateToken(1L, "admin", List.of("ROLE_ADMIN"));

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void parseToken() {
        String token = jwtService.generateToken(1L, "admin", List.of("ROLE_ADMIN"));

        Claims claims = jwtService.parseToken(token);

        assertEquals("1", claims.getSubject());
        assertEquals("admin", claims.get("username", String.class));
    }

    @Test
    void getUserId() {
        String token = jwtService.generateToken(100L, "testuser", List.of("ROLE_USER"));

        assertEquals(100L, jwtService.getUserId(token));
    }

    @Test
    void getUsername() {
        String token = jwtService.generateToken(1L, "testuser", List.of("ROLE_USER"));

        assertEquals("testuser", jwtService.getUsername(token));
    }

    @Test
    void getRoles() {
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_USER");
        String token = jwtService.generateToken(1L, "admin", roles);

        List<String> parsedRoles = jwtService.getRoles(token);

        assertEquals(2, parsedRoles.size());
        assertTrue(parsedRoles.contains("ROLE_ADMIN"));
        assertTrue(parsedRoles.contains("ROLE_USER"));
    }

    @Test
    void validateTokenWithValidToken() {
        String token = jwtService.generateToken(1L, "admin", List.of("ROLE_ADMIN"));

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateTokenWithInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.value"));
    }

    @Test
    void validateTokenWithNull() {
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void validateTokenWithEmptyString() {
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    void getExpiresIn() {
        assertEquals(EXPIRES_IN, jwtService.getExpiresIn());
    }

    @Test
    void tokenContainsCorrectClaims() {
        List<String> roles = List.of("ROLE_USER");
        String token = jwtService.generateToken(42L, "user42", roles);

        assertEquals(42L, jwtService.getUserId(token));
        assertEquals("user42", jwtService.getUsername(token));
        assertEquals(roles, jwtService.getRoles(token));
    }
}
