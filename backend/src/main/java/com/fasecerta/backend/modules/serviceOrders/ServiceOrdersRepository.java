
package com.fasecerta.backend.modules.serviceOrders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServiceOrdersRepository
        extends JpaRepository<ServiceOrdersEntity, UUID> {
}
