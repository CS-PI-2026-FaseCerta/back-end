package com.fasecerta.backend.modules.serviceOrders;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

/**
 * Contrato restrito: não expõe operações de exclusão física.
 */
public interface ServiceOrdersRepository extends Repository<ServiceOrdersEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ServiceOrdersEntity> findByIdAndDeletedAtIsNull(UUID id);

    <S extends ServiceOrdersEntity> S save(S entity);
}
