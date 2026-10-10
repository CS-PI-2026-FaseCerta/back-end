package com.fasecerta.backend.modules.user;

import com.fasecerta.backend.modules.user.UserDtos.RegisterUserRequest;
import com.fasecerta.backend.shared.enums.UserProfile;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private static final Map<String, String> CONFIG_NAMES = Map.of(
            "username", "BOOTSTRAP_ADMIN_USERNAME",
            "email", "BOOTSTRAP_ADMIN_EMAIL",
            "password", "BOOTSTRAP_ADMIN_PASSWORD");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final boolean enabled;
    private final String username;
    private final String email;
    private final String password;

    public BootstrapAdminRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Validator validator,
            @Value("${bootstrap.admin.enabled:false}") boolean enabled,
            @Value("${bootstrap.admin.username:}") String username,
            @Value("${bootstrap.admin.email:}") String email,
            @Value("${bootstrap.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.enabled = enabled;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void run(ApplicationArguments arguments) {
        if (!enabled || userRepository.countAnyAdmin() > 0) {
            return;
        }

        String normalizedUsername = username == null ? null : username.trim();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        RegisterUserRequest request = new RegisterUserRequest(
                normalizedUsername, normalizedEmail, password, UserProfile.ADMIN);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String invalidNames = violations.stream()
                    .map(violation -> CONFIG_NAMES.get(violation.getPropertyPath().toString()))
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Configuração inválida para bootstrap de ADMIN: " + invalidNames);
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL já está em uso");
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity admin = new UserEntity();
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setEmailAtivo(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setPerfil(UserProfile.ADMIN);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userRepository.saveAndFlush(admin);
    }
}
