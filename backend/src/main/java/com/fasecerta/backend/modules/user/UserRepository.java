package com.fasecerta.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    @Query(value = "SELECT COUNT(*) > 0 FROM usuarios", nativeQuery = true)
    boolean existsAnyUser();
}