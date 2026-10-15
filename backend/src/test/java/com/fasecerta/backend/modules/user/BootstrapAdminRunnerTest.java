package com.fasecerta.backend.modules.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.fasecerta.backend.shared.enums.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BootstrapAdminRunnerTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    BootstrapAdminRunnerTest() {
        validator.afterPropertiesSet();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void disabledBootstrapDoesNotTouchUsers() {
        runner(false, "Admin", "admin@example.com", "senha-segura").run(arguments());

        verifyNoInteractions(repository, passwordEncoder);
    }

    @Test
    void createsOnlyOneAdminAndEncodesPassword() {
        when(repository.countAnyAdmin()).thenReturn(0L, 1L);
        when(passwordEncoder.encode("senha-segura")).thenReturn("hash-codificado");
        BootstrapAdminRunner runner = runner(true, "  Admin  ", "  ADMIN@EXAMPLE.COM  ", "senha-segura");

        runner.run(arguments());
        runner.run(arguments());

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository, times(1)).saveAndFlush(captor.capture());
        verify(passwordEncoder, times(1)).encode("senha-segura");
        UserEntity saved = captor.getValue();
        assertEquals(UserProfile.ADMIN, saved.getPerfil());
        assertEquals("Admin", saved.getUsername());
        assertEquals("admin@example.com", saved.getEmail());
        assertEquals("admin@example.com", saved.getEmailAtivo());
        assertEquals("hash-codificado", saved.getPasswordHash());
        assertFalse(saved.getPasswordHash().equals("senha-segura"));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void existingAdminIsNeverModified() {
        when(repository.countAnyAdmin()).thenReturn(1L);

        runner(true, "", "", "").run(arguments());

        verify(repository).countAnyAdmin();
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void invalidConfigurationFailsWithoutCreatingPartialUser() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> runner(true, "Admin", "", "").run(arguments()));

        assertTrue(exception.getMessage().contains("BOOTSTRAP_ADMIN_EMAIL"));
        assertTrue(exception.getMessage().contains("BOOTSTRAP_ADMIN_PASSWORD"));
        verify(repository).countAnyAdmin();
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(passwordEncoder);
    }

    private BootstrapAdminRunner runner(boolean enabled, String username, String email, String password) {
        return new BootstrapAdminRunner(repository, passwordEncoder, validator,
                enabled, username, email, password);
    }

    private DefaultApplicationArguments arguments() {
        return new DefaultApplicationArguments(new String[0]);
    }
}
