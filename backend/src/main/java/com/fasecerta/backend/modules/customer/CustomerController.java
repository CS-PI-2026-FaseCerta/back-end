package com.fasecerta.backend.modules.customer;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerCreateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerUpdateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerResponse;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerPageResponse;
import com.fasecerta.backend.shared.enums.PersonType;
import com.fasecerta.backend.exceptions.UnauthenticatedException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerCreateRequest request,
            Authentication authentication) {

        UUID authenticatedUserId = authenticatedUserId(authentication);

        CustomerResponse response =
                customerService.create(
                        request,
                        authenticatedUserId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

        @PatchMapping("/{id}")
        public ResponseEntity<CustomerResponse> update(
                        @PathVariable UUID id,
                        @Valid @RequestBody CustomerUpdateRequest request,
                        Authentication authentication) {
                                UUID authenticatedUserId = authenticatedUserId(authentication);
                return ResponseEntity.ok(customerService.update(id, request, authenticatedUserId));
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<CustomerPageResponse> list(
                @RequestParam(defaultValue = "1") int page,
                @RequestParam(defaultValue = "10") int limit,
                @RequestParam(required = false) String nome,
                @RequestParam(required = false) String documento,
                @RequestParam(required = false) PersonType tipoPessoa,
                @RequestParam(required = false) String email) {

            return ResponseEntity.ok(customerService.list(
                    page,
                    limit,
                    nome,
                    documento,
                    tipoPessoa,
                    email
            ));
        }

        @GetMapping("/{id}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<CustomerResponse> findById(@PathVariable UUID id) {
            return ResponseEntity.ok(customerService.findById(id));
        }

        private UUID authenticatedUserId(Authentication authentication) {
                if (authentication == null
                                || !authentication.isAuthenticated()
                                || authentication instanceof AnonymousAuthenticationToken) {
                        throw new UnauthenticatedException("Usuário não autenticado");
                }

                try {
                        return UUID.fromString(authentication.getName());
                } catch (IllegalArgumentException exception) {
                        throw new UnauthenticatedException("Usuário autenticado inválido");
                }
        }
}