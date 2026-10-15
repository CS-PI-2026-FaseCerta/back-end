package com.fasecerta.backend.modules.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Conta também registros excluídos logicamente: bootstrap não recria um ADMIN já provisionado.
    @Query(value = "SELECT COUNT(*) FROM usuarios WHERE perfil = 'ADMIN'", nativeQuery = true)
    long countAnyAdmin();
}
