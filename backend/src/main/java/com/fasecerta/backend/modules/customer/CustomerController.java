package com.fasecerta.backend.modules.customer;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerCreateRequest;
import com.fasecerta.backend.modules.customer.CustomerDtos.CustomerResponse;

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

        UUID authenticatedUserId =
                UUID.fromString(authentication.getName());

        CustomerResponse response =
                customerService.create(
                        request,
                        authenticatedUserId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}