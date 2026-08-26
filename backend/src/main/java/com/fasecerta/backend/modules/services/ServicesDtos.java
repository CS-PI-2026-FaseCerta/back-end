package com.fasecerta.backend.modules.services;

import com.fasecerta.backend.shared.enums.BillingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.math.BigDecimal;
import java.util.UUID;

public final class ServicesDtos {

    private ServicesDtos() {
    }

    public record CreateServiceRequest(

            @NotBlank(message = "nome é obrigatório") 
            String nome,

            String descricao,

            @NotBlank(message = "categoria é obrigatória") 
            String categoria,

            @NotNull(message = "tipo_cobranca é obrigatório") 
            BillingType tipo_cobranca,

            @NotNull(message = "valor_base é obrigatório") 
            @DecimalMin(value = "0.00", inclusive = true, message = "valor_base não pode ser negativo") 
            @Digits(integer = 17, fraction = 2, message = "valor_base deve possuir no máximo duas casas decimais") 
            BigDecimal valor_base,

            @Null(message = "created_by é preenchido automaticamente pelo usuário autenticado") 
            UUID created_by
    ) {
    }
}