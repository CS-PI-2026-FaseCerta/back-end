package com.fasecerta.backend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasecerta.backend.modules.auth.controller.AuthController;
import com.fasecerta.backend.modules.auth.dto.LoginResponse;
import com.fasecerta.backend.modules.auth.security.JwtService;
import com.fasecerta.backend.modules.auth.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, SecurityConfigTest.ProtectedController.class})
@TestPropertySource(properties = {
        "jwt.secret=security-config-test-secret-with-32-bytes-12345",
        "jwt.expiration=3600000"
})
class SecurityConfigTest {
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
    void protectedRouteRejectsMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer broken.token.value"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/test/protected").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteAcceptsValidToken() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), "ADMIN");

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/test/protected")
        String protectedRoute() {
            return "ok";
        }
    }
}
