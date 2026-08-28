package com.fasecerta.backend.modules.services;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServicesRepository extends JpaRepository<ServicesEntity, UUID> {
    Optional<ServicesEntity> findByIdAndDeletedAtIsNull(UUID id);
}