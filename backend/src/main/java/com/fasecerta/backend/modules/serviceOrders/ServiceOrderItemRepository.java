
package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ServiceOrderItemRepository
        extends JpaRepository<ServiceOrderItemEntity, UUID> {

    List<ServiceOrderItemEntity> findAllByOrdemServico_Id(UUID ordemServicoId);

    @Query("""
            SELECT COALESCE(SUM(item.valorTotalItem), 0)
            FROM ServiceOrderItemEntity item
            WHERE item.ordemServico.id = :ordemServicoId
            """)
    BigDecimal calcularSubtotal(@Param("ordemServicoId") UUID ordemServicoId);
}
