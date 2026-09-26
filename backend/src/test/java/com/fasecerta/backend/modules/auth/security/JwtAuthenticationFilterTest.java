package com.fasecerta.backend.modules.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
    private final JwtService jwtService = new JwtService("jwt-filter-test-secret-with-32-bytes-12345", 3_600_000);
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
    void validBearerTokenAuthenticatesWithUuidAndRoleButNoCredentials() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(userId, "GESTOR"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(userId.toString(), authentication.getName());
        assertEquals("GESTOR", authentication.getAuthorities().iterator().next().getAuthority());
        assertNull(authentication.getCredentials());
    }

    @Test
    void invalidExpiredAndNonBearerHeadersDoNotAuthenticate() throws Exception {
        JwtService expiredIssuer = new JwtService("jwt-filter-test-secret-with-32-bytes-12345", 1);
        String expiredToken = expiredIssuer.generateToken(UUID.randomUUID(), "ADMIN");
        Thread.sleep(1_100);

        for (String authorization : new String[] {
                "Bearer broken.token.value",
                "Bearer " + expiredToken,
                "Basic abc"
        }) {
            SecurityContextHolder.clearContext();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", authorization);
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}
