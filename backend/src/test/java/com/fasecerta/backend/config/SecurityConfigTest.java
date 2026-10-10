package com.fasecerta.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasecerta.backend.modules.auth.controller.AuthController;
import com.fasecerta.backend.modules.auth.dto.LoginResponse;
import com.fasecerta.backend.modules.auth.ratelimit.LoginRateLimiter;
import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.modules.auth.service.AuthService;
import com.fasecerta.backend.modules.user.UserController;
import com.fasecerta.backend.modules.user.UserDtos.UserResponse;
import com.fasecerta.backend.modules.user.UserService;
import com.fasecerta.backend.shared.enums.UserProfile;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest({AuthController.class, UserController.class})
@Import({SecurityConfig.class, JwtService.class, LoginRateLimiter.class,
        SecurityConfigTest.ProtectedController.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "jwt.expiration=60000",
        "auth.rate-limit.max-attempts=2",
        "auth.rate-limit.window-seconds=60"
})
class SecurityConfigTest {
    @Value("${jwt.secret}")
    private String testSecret;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

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
    void configuredRateLimitAppliesOnlyToLogin() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("signed-token"));
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("192.0.2.55");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"usuario@email.com\",\"password\":\"senha\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.55");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"usuario@email.com\",\"password\":\"senha\"}"))
                .andExpect(status().isTooManyRequests());

        String token = jwtService.generateToken(UUID.randomUUID(), "ADMIN");
        mockMvc.perform(get("/test/protected")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.55");
                            return request;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
        assertUnauthorized("Token abc");
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

    @Test
    void passwordRecoveryRoutesArePublicEvenBeforeTheirControllersExist() throws Exception {
        for (String path : new String[] {
                "/api/auth/forgot-password", "/api/auth/reset-password"
        }) {
            mockMvc.perform(post(path))
                    .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
        }
    }

    @Test
    void oldRegisterRouteIsNotPublic() throws Exception {
        mockMvc.perform(post("/api/auth/register"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCreationWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Não autenticado"));
        verify(userService, never()).registerUser(any(), any());
    }

    @Test
    void tecnicoCannotCreateUsers() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", bearerToken("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));
        verify(userService, never()).registerUser(any(), any());
    }

    @Test
    void adminAndGestorReachUserCreationService() throws Exception {
        when(userService.registerUser(any(), any()))
                .thenReturn(new UserResponse(UUID.randomUUID(), "Novo", "novo@example.com",
                        UserProfile.TECNICO, LocalDateTime.now()));

        for (String role : new String[] {"ADMIN", "GESTOR"}) {
            mockMvc.perform(post("/api/usuarios")
                            .header("Authorization", bearerToken(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationBody()))
                    .andExpect(status().isCreated());
        }
        verify(userService, times(2)).registerUser(any(), any());
    }

    @Test
    void unknownSignedRoleCannotAuthenticate() throws Exception {
        Instant issuedAt = Instant.now();
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "SUPER_ADMIN")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertUnauthorized("Bearer " + token);
    }

    @Test
    void corsAllowsConfiguredOriginAndRejectsOthers() throws Exception {
        mockMvc.perform(options("/api/usuarios")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/usuarios")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void corsConfigurationParsesMultipleOrigins() {
        var source = new SecurityConfig().corsConfigurationSource(
                "http://localhost:5173, https://app.example.com ");
        var configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertEquals(List.of("http://localhost:5173", "https://app.example.com"),
                configuration.getAllowedOrigins());
    }

    private String bearerToken(String role) {
        return "Bearer " + jwtService.generateToken(UUID.randomUUID(), role);
    }

    private String registrationBody() {
        return "{\"username\":\"Novo\",\"email\":\"novo@example.com\","
                + "\"password\":\"senha123\",\"perfil\":\"TECNICO\"}";
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
