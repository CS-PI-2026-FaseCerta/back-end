package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
public class ServiceOrdersController {

    private final ServiceOrdersService serviceOrdersService;

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable UUID id, Authentication authentication) {
        UUID authenticatedUserId = authenticatedUserId(authentication);
        serviceOrdersService.delete(id, authenticatedUserId);
        return ResponseEntity.ok(Map.of("message", "Ordem de Serviço removida com sucesso"));
    }

    private UUID authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
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
