package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOrdersItemRepository
        extends JpaRepository<ServiceOrdersItemEntity, UUID> {

    List<ServiceOrdersItemEntity> findAllByOrdem_IdOrderByCreatedAtAscIdAsc(UUID ordemId);

    Optional<ServiceOrdersItemEntity> findByIdAndOrdem_Id(UUID itemId, UUID ordemId);
}