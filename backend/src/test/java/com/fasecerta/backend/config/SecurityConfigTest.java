package com.fasecerta.backend.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasecerta.backend.modules.auth.controller.AuthController;
import com.fasecerta.backend.modules.auth.dto.LoginResponse;
import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.modules.auth.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, SecurityConfigTest.ProtectedController.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "jwt.expiration=60000")
class SecurityConfigTest {
    @Value("${jwt.secret}")
    private String testSecret;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginIsPublicAndDoesNotRequireCsrfToken() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("signed-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usuario@email.com\",\"password\":\"senha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-token"));
    }

    @Test
    void protectedRouteWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Não autenticado"));
    }

    @Test
    void protectedRouteWithExpiredTokenReturnsUnauthorized() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(120);
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "ADMIN")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertUnauthorized("Bearer " + token);
    }

    @Test
    void protectedRouteWithTamperedTokenReturnsUnauthorized() throws Exception {
        String[] parts = jwtService.generateToken(UUID.randomUUID(), "TECNICO").split("\\.");
        parts[2] = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);

        assertUnauthorized("Bearer " + String.join(".", parts));
    }

    @Test
    void protectedRouteWithDifferentSigningKeyReturnsUnauthorized() throws Exception {
        String token = new JwtService("different-security-config-test-secret-12345", 60_000)
                .generateToken(UUID.randomUUID(), "GESTOR");

        assertUnauthorized("Bearer " + token);
    }

    @Test
    void protectedRouteDoesNotAcceptBasicAuthorization() throws Exception {
        assertUnauthorized("Basic abc");
    }

    @Test
    void protectedRouteRejectsMalformedBearerHeaders() throws Exception {
        assertUnauthorized("Bearer");
        assertUnauthorized("Bearer invalid-token");
    }

    @Test
    void configuredExpirationControlsJwtLifetime() {
        String token = jwtService.generateToken(UUID.randomUUID(), "ADMIN");
        JwtService.JwtClaims claims = jwtService.parseValidToken(token).orElseThrow();
        long lifetimeMillis = Duration.between(claims.issuedAt(), claims.expiresAt()).toMillis();

        assertTrue(lifetimeMillis >= 59_000 && lifetimeMillis <= 61_000);
    }

    @Test
    void protectedRouteAcceptsValidToken() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), "ADMIN");

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    private void assertUnauthorized(String authorization) throws Exception {
        mockMvc.perform(get("/test/protected").header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Não autenticado"));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/test/protected")
        String protectedRoute() {
            return "ok";
        }
    }
}
