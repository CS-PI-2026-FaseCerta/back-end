package com.fasecerta.backend.modules.auth.port;

import java.util.Optional;

public interface AuthenticationUserProvider {
    Optional<AuthenticatedUser> findByEmail(String email);
}
