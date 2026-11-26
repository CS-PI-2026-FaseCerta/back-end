package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceOrdersRepository
        extends JpaRepository<ServiceOrdersEntity, UUID> {

    Optional<ServiceOrdersEntity> findByNumeroOs(Long numeroOs);

    Optional<ServiceOrdersEntity> findByIdAndDeletedAtIsNull(UUID id);
}
