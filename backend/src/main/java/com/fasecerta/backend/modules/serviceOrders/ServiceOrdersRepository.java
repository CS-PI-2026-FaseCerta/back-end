package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ServiceOrdersRepository
        extends JpaRepository<ServiceOrdersEntity, UUID> {

    Optional<ServiceOrdersEntity> findByNumeroOs(Long numeroOs);

    Optional<ServiceOrdersEntity> findByIdAndDeletedAtIsNull(UUID id);

    @NativeQuery(value = """
            SELECT os.id, os.numero_os, os.cliente_id,
                   CASE WHEN c.tipo_pessoa = 'PJ' THEN c.razao_social ELSE c.nome_completo END AS cliente_nome,
                   os.status, os.valor_total, os.created_at
            FROM ordens_servico os
            JOIN customer c ON c.id = os.cliente_id
            WHERE os.deleted_at IS NULL
            ORDER BY os.numero_os DESC
            """, countQuery = "SELECT COUNT(*) FROM ordens_servico WHERE deleted_at IS NULL",
            sqlResultSetMapping = "ServiceOrderSummary")
    Page<ServiceOrdersDtos.ServiceOrderSummary> findActiveSummaries(Pageable pageable);

    @Query(value = """
            SELECT CASE WHEN c.tipo_pessoa = 'PJ' THEN c.razao_social ELSE c.nome_completo END
            FROM ordens_servico os
            JOIN customer c ON c.id = os.cliente_id
            WHERE os.id = :orderId AND os.deleted_at IS NULL
            """, nativeQuery = true)
    String findHistoricalCustomerName(UUID orderId);
}
