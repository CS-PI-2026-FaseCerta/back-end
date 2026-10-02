package com.fasecerta.backend.modules.customer;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {
    Optional<CustomerEntity> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByCpfAndDeletedAtIsNull(String cpf);
    boolean existsByCpfAndDeletedAtIsNullAndIdNot(String cpf, UUID id);

    boolean existsByCnpjAndDeletedAtIsNull(String cnpj);
    boolean existsByCnpjAndDeletedAtIsNullAndIdNot(String cnpj, UUID id);

    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, UUID id);
} 
