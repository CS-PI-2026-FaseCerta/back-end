package com.fasecerta.backend.modules.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasecerta.backend.shared.enums.BillingType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ServicesDtos {

    private ServicesDtos() {
    }

    public record CreateServiceRequest(

            @NotBlank(message = "nome é obrigatório")
            @Size(max = 255, message = "nome não pode exceder 255 caracteres")
            String nome,

            @Size(max = 1000, message = "descricao não pode exceder 1000 caracteres")
            String descricao,

            @NotBlank(message = "categoria é obrigatória")
            @Size(max = 255, message = "categoria não pode exceder 255 caracteres")
            String categoria,

            @NotNull(message = "tipo_cobranca é obrigatório")
            BillingType tipo_cobranca,

            @NotNull(message = "valor_base é obrigatório")
            @DecimalMin(
                    value = "0.00",
                    inclusive = true,
                    message = "valor_base não pode ser negativo"
            )
            @Digits(
                    integer = 17,
                    fraction = 2,
                    message = "valor_base deve possuir no máximo duas casas decimais"
            )
            BigDecimal valor_base
    ) {
    }

    public record ServiceResponse(

            UUID id,
            String nome,
            String descricao,
            String categoria,
            BillingType tipo_cobranca,
            BigDecimal valor_base,
            UUID created_by,
            UUID updated_by,
            LocalDateTime created_at,
            LocalDateTime updated_at
    ) {
    }

    public record ServicePageResponse(

            List<ServiceResponse> items,
            long total,
            int page,
            int limit,
            int totalPages
    ) {
    }
}