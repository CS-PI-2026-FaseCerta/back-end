package com.fasecerta.backend.modules.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasecerta.backend.shared.enums.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "bootstrap.admin.enabled=true",
        "bootstrap.admin.username=Primeiro Admin",
        "bootstrap.admin.email=primeiro-admin@example.com",
        "bootstrap.admin.password=senha-inicial-segura"
})
@ActiveProfiles("test")
class BootstrapAdminIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BootstrapAdminRunner bootstrapAdminRunner;

    @Test
    void startupCreatesHashedAdminOnceInTestDatabase() {
        UserEntity admin = userRepository.findByEmailAndDeletedAtIsNull("primeiro-admin@example.com")
                .orElseThrow();

        assertEquals(UserProfile.ADMIN, admin.getPerfil());
        assertEquals(1L, userRepository.countAnyAdmin());
        assertTrue(passwordEncoder.matches("senha-inicial-segura", admin.getPasswordHash()));

        bootstrapAdminRunner.run(new DefaultApplicationArguments(new String[0]));
        assertEquals(1L, userRepository.countAnyAdmin());
    }
}
