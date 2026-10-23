package com.fasecerta.backend.modules.paymentmethods;

import com.fasecerta.backend.shared.enums.PaymentMethod;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceOrderPaymentMethodRepository
        extends JpaRepository<ServiceOrderPaymentMethodEntity, UUID> {

    List<ServiceOrderPaymentMethodEntity> findAllByOsId(UUID osId);

    boolean existsByOsIdAndMeioPagamento(UUID osId, PaymentMethod meioPagamento);

    @Modifying(flushAutomatically = true)
    @Query("delete from ServiceOrderPaymentMethodEntity method where method.osId = :osId")
    void deleteByOsId(@Param("osId") UUID osId);
}
