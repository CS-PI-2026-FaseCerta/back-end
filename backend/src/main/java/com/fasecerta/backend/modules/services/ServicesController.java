package com.fasecerta.backend.modules.services;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasecerta.backend.modules.services.ServicesDtos.CreateServiceRequest;
import com.fasecerta.backend.modules.services.ServicesDtos.DeleteServiceResponse;
import com.fasecerta.backend.modules.services.ServicesDtos.ServicePageResponse;
import com.fasecerta.backend.modules.services.ServicesDtos.ServiceResponse;
import com.fasecerta.backend.shared.enums.BillingType;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicos")
public class ServicesController {

        private final ServicesService servicesService;

        public ServicesController(ServicesService servicesService) {
                this.servicesService = servicesService;
        }

        @PostMapping
        @PreAuthorize("isAuthenticated() and (hasAuthority('ADMIN') or hasRole('ADMIN') or hasAuthority('GESTOR') or hasRole('GESTOR'))")
        public ResponseEntity<ServiceResponse> create(
                        @Valid @RequestBody CreateServiceRequest request,
                        Authentication authentication) {
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(servicesService.create(request, authentication));
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

        @DeleteMapping("/{id}")
        @PreAuthorize("isAuthenticated() and (hasAuthority('ADMIN') or hasRole('ADMIN') or hasAuthority('GESTOR') or hasRole('GESTOR'))")
        public ResponseEntity<DeleteServiceResponse> remove(
                        @PathVariable UUID id,
                        Authentication authentication) {
                servicesService.remove(id, authentication);
                return ResponseEntity.ok(new DeleteServiceResponse("Serviço removido com sucesso"));
        }
}