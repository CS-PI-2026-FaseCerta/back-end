package com.fasecerta.backend.modules.auth.port;

import java.util.Optional;

/**
 * Contrato da autenticação para consultar somente os dados necessários do usuário.
 * A implementação concreta pertence ao módulo de usuários; este contrato não é uma entidade JPA.
 */
public interface AuthenticationUserProvider {
    Optional<AuthenticatedUser> findByEmail(String email);
}
