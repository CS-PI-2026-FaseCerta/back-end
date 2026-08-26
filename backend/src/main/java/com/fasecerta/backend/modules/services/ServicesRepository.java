package com.fasecerta.backend.modules.services;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServicesRepository extends JpaRepository<ServicesEntity, UUID> {
}