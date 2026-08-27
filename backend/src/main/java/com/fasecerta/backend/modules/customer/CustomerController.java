package com.fasecerta.backend.modules.customer;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerCreateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerUpdateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerResponse;
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