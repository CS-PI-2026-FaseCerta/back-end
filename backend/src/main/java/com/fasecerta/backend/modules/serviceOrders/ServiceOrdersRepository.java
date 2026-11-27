package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT os FROM ServiceOrdersEntity os WHERE os.id = :id AND os.deletedAt IS NULL")
    Optional<ServiceOrdersEntity> findActiveByIdForUpdate(UUID id);

    // Operações em lote ignoram os callbacks da entidade; a remoção deve passar pelo serviço.
    @Override
    default void deleteAllByIdInBatch(Iterable<UUID> ids) {
        throw new UnsupportedOperationException("Utilize ServiceOrdersService.remover");
    }

    @Override
    default void deleteAllInBatch(Iterable<ServiceOrdersEntity> orders) {
        throw new UnsupportedOperationException("Utilize ServiceOrdersService.remover");
    }

    @Override
    default void deleteAllInBatch() {
        throw new UnsupportedOperationException("Utilize ServiceOrdersService.remover");
    }

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
