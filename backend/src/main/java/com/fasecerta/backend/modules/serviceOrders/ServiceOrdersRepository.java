package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ServiceOrdersRepository
        extends JpaRepository<ServiceOrdersEntity, UUID> {

    Optional<ServiceOrdersEntity> findByNumeroOs(Long numeroOs);

    Optional<ServiceOrdersEntity> findByIdAndDeletedAtIsNull(UUID id);

    // A exclusão lógica do cliente não deve ocultar seu nome na consulta de uma OS ativa.
    @Query(value = """
            SELECT CASE WHEN c.tipo_pessoa = 'PJ' THEN c.razao_social ELSE c.nome_completo END
            FROM ordens_servico os
            JOIN customer c ON c.id = os.cliente_id
            WHERE os.id = :orderId AND os.deleted_at IS NULL
            """, nativeQuery = true)
    String findHistoricalCustomerName(UUID orderId);
}
