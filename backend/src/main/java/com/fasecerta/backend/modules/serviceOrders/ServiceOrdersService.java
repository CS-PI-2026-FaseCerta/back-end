package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.exceptions.UnauthenticatedException;
import com.fasecerta.backend.shared.enums.StatusOS;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ServiceOrdersService {

    private final ServiceOrdersRepository serviceOrdersRepository;

    @Transactional
    public void delete(UUID id, UUID authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw new UnauthenticatedException("Usuário não autenticado");
        }

        // Bloqueia a OS durante a validação e a atualização, impedindo duas remoções concorrentes.
        ServiceOrdersEntity order = serviceOrdersRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de Serviço não encontrada"));

        if (order.getStatus() != StatusOS.EM_ABERTO && order.getStatus() != StatusOS.NAO_APROVADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ordem de Serviço não pode ser removida neste estado");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setDeletedAt(now);
        order.setUpdatedAt(now);
        order.setUpdatedBy(authenticatedUserId);
        // Não remove a OS nem os seus registros relacionados; mantém o histórico no banco.
        serviceOrdersRepository.save(order);
    }
}
