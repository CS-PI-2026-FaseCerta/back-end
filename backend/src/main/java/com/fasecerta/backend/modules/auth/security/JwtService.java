package com.fasecerta.backend.modules.auth.security;

import com.fasecerta.backend.shared.enums.UserProfile;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expirationMillis) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve ter pelo menos 32 bytes UTF-8");
        }
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION deve ser positivo");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(UUID userId, String role) {
        String acceptedRole = UserProfile.valueOf(role).name();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expirationMillis);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", acceptedRole)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<JwtClaims> parseValidToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subjectClaim = claims.getSubject();
            String roleClaim = claims.get("role", String.class);
            Date issuedAt = claims.getIssuedAt();
            Date expiresAt = claims.getExpiration();
            if (subjectClaim == null || roleClaim == null || issuedAt == null || expiresAt == null
                    || issuedAt.after(new Date()) || !expiresAt.after(issuedAt)) {
                return Optional.empty();
            }
            UUID subject = UUID.fromString(subjectClaim);
            String role = UserProfile.valueOf(roleClaim).name();
            return Optional.of(new JwtClaims(subject, role, issuedAt.toInstant(), expiresAt.toInstant()));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean isValid(String token) {
        return parseValidToken(token).isPresent();
    }

    public record JwtClaims(UUID subject, String role, Instant issuedAt, Instant expiresAt) {
    }
}
