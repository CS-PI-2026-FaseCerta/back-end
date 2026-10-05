package com.fasecerta.backend.modules.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "jwt-test-secret-with-at-least-32-bytes-12345";
    private final JwtService jwtService = new JwtService(SECRET, 3_600_000);

    @Test
    void expirationBelowOneSecondIsRejectedAtConstruction() {
        for (long expirationMillis : new long[] { -1, 0, 1, 999 }) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new JwtService(SECRET, expirationMillis));
            assertEquals("JWT_EXPIRATION deve ser de no mínimo 1000 ms", exception.getMessage());
        }
    }

    @Test
    void expirationFromOneSecondIsAccepted() {
        for (long expirationMillis : new long[] { 1_000, 1_001, 3_600_000 }) {
            JwtService service = assertDoesNotThrow(() -> new JwtService(SECRET, expirationMillis));
            assertNotNull(service.generateToken(UUID.randomUUID(), "ADMIN"));
        }
    }

    @Test
    void generatedTokenContainsRequiredClaimsAndIsValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "ADMIN");

        assertEquals(3, token.split("\\.").length);
        assertTrue(jwtService.isValid(token));
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void tamperedTokenIsRejected() {
        String[] parts = jwtService.generateToken(UUID.randomUUID(), "TECNICO").split("\\.");
        parts[2] = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);

        assertFalse(jwtService.isValid(String.join(".", parts)));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-jwt-test-secret-with-32-bytes-12345", 3_600_000)
                .generateToken(UUID.randomUUID(), "GESTOR");

        assertFalse(jwtService.isValid(token));
    }

    @Test
    void expiredTokenIsRejected() {
        Instant past = Instant.now().minusSeconds(120);
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "ADMIN")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertFalse(jwtService.isValid(token));
    }

    @Test
    void malformedAndIncompleteTokensAreRejected() {
        assertFalse(jwtService.isValid("not-a-jwt"));
        String tokenWithoutRole = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
        assertFalse(jwtService.isValid(tokenWithoutRole));
    }
}
