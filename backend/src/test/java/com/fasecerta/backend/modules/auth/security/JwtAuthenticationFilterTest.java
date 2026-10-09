package com.fasecerta.backend.modules.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "jwt-filter-test-secret-with-32-bytes-12345";
    private final JwtService jwtService = new JwtService(SECRET, 3_600_000);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingTokenDoesNotAuthenticate() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validBearerTokensMapProfilesToSpringRoles() throws Exception {
        for (String role : new String[] {"ADMIN", "GESTOR", "TECNICO"}) {
            SecurityContextHolder.clearContext();
            UUID userId = UUID.randomUUID();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + jwtService.generateToken(userId, role));

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication);
            assertEquals(userId.toString(), authentication.getName());
            assertEquals("ROLE_" + role, authentication.getAuthorities().iterator().next().getAuthority());
            assertNull(authentication.getCredentials());
        }
    }

    @Test
    void invalidExpiredAndNonBearerHeadersDoNotAuthenticate() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(120);
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "ADMIN")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
        String otherKeyToken = new JwtService("different-jwt-filter-test-secret-12345", 3_600_000)
                .generateToken(UUID.randomUUID(), "ADMIN");
        String[] tamperedParts = jwtService.generateToken(UUID.randomUUID(), "ADMIN").split("\\.");
        tamperedParts[2] = (tamperedParts[2].charAt(0) == 'A' ? "B" : "A") + tamperedParts[2].substring(1);
        String unknownRoleToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "SUPER_ADMIN")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        for (String authorization : new String[] {
                "Bearer broken.token.value",
                "Bearer " + expiredToken,
                "Bearer " + otherKeyToken,
                "Bearer " + String.join(".", tamperedParts),
                "Bearer " + unknownRoleToken,
                "Basic abc",
                "Token abc",
                "Bearer",
                "Bearer invalid-token"
        }) {
            SecurityContextHolder.clearContext();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", authorization);
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}
