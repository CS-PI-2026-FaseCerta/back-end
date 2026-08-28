package com.fasecerta.backend.modules.services;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasecerta.backend.modules.services.ServicesDtos.CreateServiceRequest;
import com.fasecerta.backend.modules.services.ServicesDtos.ServicePageResponse;
import com.fasecerta.backend.modules.services.ServicesDtos.ServiceResponse;
import com.fasecerta.backend.shared.enums.BillingType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicesController {

        private final ServicesService servicesService;

        @PostMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
        public ResponseEntity<ServiceResponse> create(
                        @Valid @RequestBody CreateServiceRequest request,
                        Authentication authentication) {

                ServiceResponse response = servicesService.create(
                                request,
                                authentication);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ServicePageResponse> list(
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int limit,
                        @RequestParam(required = false) String nome,
                        @RequestParam(required = false) String categoria,
                        @RequestParam(name = "tipo_cobranca", required = false) BillingType tipoCobranca,
                        @RequestParam(name = "order_by", required = false) String orderBy,
                        @RequestParam(name = "order_dir", defaultValue = "asc") String orderDir) {
                return ResponseEntity.ok(servicesService.list(
                                page,
                                limit,
                                nome,
                                categoria,
                                tipoCobranca,
                                orderBy,
                                orderDir));
        }

        @GetMapping("/{id}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ServiceResponse> findById(@PathVariable UUID id) {
                return ResponseEntity.ok(servicesService.findById(id));
        }
}