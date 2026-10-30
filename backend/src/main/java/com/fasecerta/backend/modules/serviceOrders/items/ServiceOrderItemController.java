
package com.fasecerta.backend.modules.serviceOrders.items;

import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.AddServiceOrderItemRequest;
import com.fasecerta.backend.modules.serviceOrders.items.dto.ServiceOrderItemDtos.ServiceOrderItemResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ordens-servico/{ordemServicoId}/itens")
public class ServiceOrderItemController {

    private final ServiceOrderItemService itemService;

    public ServiceOrderItemController(ServiceOrderItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceOrderItemResponse> addItem(
            @PathVariable UUID ordemServicoId,
            @Valid @RequestBody AddServiceOrderItemRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(itemService.addItem(ordemServicoId, request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceOrderItemResponse>> listItems(
            @PathVariable UUID ordemServicoId) {
        return ResponseEntity.ok(itemService.listItems(ordemServicoId));
    }

    @GetMapping("/subtotal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BigDecimal> calculateSubtotal(
            @PathVariable UUID ordemServicoId) {
        return ResponseEntity.ok(
                itemService.calculateSubtotal(ordemServicoId));
    }
}
