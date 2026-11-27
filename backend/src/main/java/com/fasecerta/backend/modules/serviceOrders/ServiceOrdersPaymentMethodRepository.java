package com.fasecerta.backend.modules.serviceOrders;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOrdersPaymentMethodRepository extends JpaRepository<ServiceOrdersPaymentMethodEntity, UUID> {

    List<ServiceOrdersPaymentMethodEntity> findAllByOrdem_Id(UUID ordemId);
}