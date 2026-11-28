package com.fasecerta.backend.modules.serviceOrders.items.dto;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceQuantityDeserializer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ServiceOrderItemDtos {

        private ServiceOrderItemDtos() {
        }

        public record AddServiceOrderItemRequest(

                        @NotNull(message = "O serviço é obrigatório") UUID servicoId,

                        @NotNull(message = "A quantidade é obrigatória")
                        @Positive(message = "A quantidade deve ser maior que zero")
                        @Max(value = ServiceItemRequest.MAX_QUANTITY, message = "A quantidade aceita até 15 dígitos")
                        @JsonDeserialize(using = ServiceQuantityDeserializer.class)
                        Long quantidade,

                        @DecimalMin(value = "0.00", message = "O valor unitário não pode ser negativo") @Digits(integer = 17, fraction = 2, message = "O valor unitário deve ter no máximo duas casas decimais") BigDecimal valorUnitarioCobrado,

                        @Size(max = 5000, message = "A observação deve ter no máximo 5000 caracteres") String observacao

        ) {
                public AddServiceOrderItemRequest(UUID servicoId, Integer quantidade,
                                BigDecimal valorUnitarioCobrado, String observacao) {
                        this(servicoId, quantidade == null ? null : quantidade.longValue(),
                                        valorUnitarioCobrado, observacao);
                }
        }

        public record ServiceOrderItemResponse(
                        UUID id,
                        UUID servicoId,
                        Long quantidade,
                        BigDecimal valorUnitarioCobrado,
                        BigDecimal valorTotalItem,
                        String observacao,
                        LocalDateTime createdAt) {
        }
}
