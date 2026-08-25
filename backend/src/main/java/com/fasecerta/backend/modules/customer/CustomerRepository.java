package com.fasecerta.backend.modules.customer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fasecerta.backend.modules.customer.CustomerEntity;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {
    boolean existsByCpfAndDeletedAtIsNull(String cpf);

    boolean existsByCnpjAndDeletedAtIsNull(String cnpj);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    
} 
