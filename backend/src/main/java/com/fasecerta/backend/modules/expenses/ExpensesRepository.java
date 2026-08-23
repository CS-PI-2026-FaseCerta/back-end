package com.fasecerta.backend.modules.expenses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpensesRepository extends JpaRepository<ExpensesEntity, UUID> {
}
