package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.CreateServiceOrderRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderResponse;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderDetailResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceOrderPage;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ServiceOrderPage list(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int limit) {
        return service.list(page, limit);
    }

    @GetMapping("/{id}")
    public ServiceOrderDetailResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceOrderResponse cadastrar(@Valid @RequestBody CreateServiceOrderRequest request, Authentication authentication) {

        return service.cadastrar(request, authentication);
    }
}
