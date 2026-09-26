package com.fasecerta.backend.modules.auth.port;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String passwordHash, String role) {
}
