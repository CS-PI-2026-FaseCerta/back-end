package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.CreateServiceOrderRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderResponse;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderDetailResponse;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.UpdateServiceOrderRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.UpdateStatusRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderPage;
import com.fasecerta.backend.shared.enums.StatusOS;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordens-servico")
public class ServiceOrdersController {

    private final ServiceOrdersService service;

    public ServiceOrdersController(ServiceOrdersService service) {
        this.service = service;
    }

    @GetMapping
    public ServiceOrderPage list(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "numeroOs", required = false) Long orderNumber,
            @RequestParam(required = false) StatusOS status) {
        return service.list(page, limit, orderNumber, status);
    }

    @GetMapping("/{id}")
    public ServiceOrderDetailResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void remover(@PathVariable UUID id, Authentication authentication) {
        service.remover(id, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceOrderResponse cadastrar(@Valid @RequestBody CreateServiceOrderRequest request, Authentication authentication) {

        return service.cadastrar(request, authentication);
    }

    @PutMapping("/{id}")
    public ServiceOrderDetailResponse atualizar(
            @PathVariable UUID id, @Valid @RequestBody UpdateServiceOrderRequest request,
            Authentication authentication) {
        return service.atualizar(id, request, authentication);
    }

    @PatchMapping("/{id}/status")
    public ServiceOrderDetailResponse alterarStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        return service.alterarStatus(id, request.status(), authentication);
    }
}
