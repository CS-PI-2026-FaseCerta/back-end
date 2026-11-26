package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOrdersRepository
        extends JpaRepository<ServiceOrdersEntity, UUID> {

    Optional<ServiceOrdersEntity> findByNumeroOs(Long numeroOs);

    Optional<ServiceOrdersEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Bulk operations bypass the entity's @SQLDelete.
    String MARK_ACTIVE_ORDERS_AS_DELETED =
            "UPDATE ServiceOrdersEntity os SET os.deletedAt = LOCAL DATETIME WHERE os.deletedAt IS NULL";

    @Override
    default void deleteAllByIdInBatch(Iterable<UUID> ids) {
        markDeletedByIds(Streamable.of(ids).toList());
    }

    @Override
    default void deleteAllInBatch(Iterable<ServiceOrdersEntity> orders) {
        markDeletedByIds(Streamable.of(orders).map(ServiceOrdersEntity::getId).toList());
    }

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(MARK_ACTIVE_ORDERS_AS_DELETED + " AND os.id IN :ids")
    void markDeletedByIds(List<UUID> ids);

    @Override
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(MARK_ACTIVE_ORDERS_AS_DELETED)
    void deleteAllInBatch();

    String ACTIVE_SUMMARY_FILTERS = """
            WHERE os.deleted_at IS NULL
              AND (:orderNumber IS NULL OR os.numero_os = :orderNumber)
              AND (:status IS NULL OR os.status = :status)
            """;

    @NativeQuery(value = """
            SELECT os.id, os.numero_os, os.cliente_id,
                   CASE WHEN c.tipo_pessoa = 'PJ' THEN c.razao_social ELSE c.nome_completo END AS cliente_nome,
                   os.status, os.valor_total, os.created_at
            FROM ordens_servico os
            JOIN customer c ON c.id = os.cliente_id
            """ + ACTIVE_SUMMARY_FILTERS + "ORDER BY os.numero_os DESC",
            countQuery = "SELECT COUNT(*) FROM ordens_servico os " + ACTIVE_SUMMARY_FILTERS,
            sqlResultSetMapping = "ServiceOrderSummary")
    Page<ServiceOrdersDtos.ServiceOrderSummary> findActiveSummaries(Long orderNumber, String status, Pageable pageable);

    @Query(value = """
            SELECT CASE WHEN c.tipo_pessoa = 'PJ' THEN c.razao_social ELSE c.nome_completo END
            FROM ordens_servico os
            JOIN customer c ON c.id = os.cliente_id
            WHERE os.id = :orderId AND os.deleted_at IS NULL
            """, nativeQuery = true)
    String findHistoricalCustomerName(UUID orderId);
}
