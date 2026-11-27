package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOrdersNumberRepository extends JpaRepository<ServiceOrdersNumberEntity, Long> {
}