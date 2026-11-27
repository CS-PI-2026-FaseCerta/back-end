package com.fasecerta.backend.modules.serviceOrders;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceOrdersPaymentMethodRepository extends JpaRepository<ServiceOrdersPaymentMethodEntity, UUID> {

    List<ServiceOrdersPaymentMethodEntity> findAllByOrdem_Id(UUID ordemId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ServiceOrdersPaymentMethodEntity method where method.ordem.id = :ordemId")
    void deleteByOrdemId(@Param("ordemId") UUID ordemId);
}
