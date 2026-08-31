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
import jakarta.validation.constraints.Null;

public final class ServicesDtos {

        private ServicesDtos() {
        }

        public record CreateServiceRequest(
                        @NotBlank(message = "nome é obrigatório") String nome,

                        String descricao,

                        @NotBlank(message = "categoria é obrigatória") String categoria,

                        @NotNull(message = "tipo_cobranca é obrigatório") BillingType tipo_cobranca,

                        @NotNull(message = "valor_base é obrigatório") @DecimalMin(value = "0.0", inclusive = true, message = "valor_base não pode ser negativo") @Digits(integer = 17, fraction = 2, message = "valor_base deve possuir no máximo duas casas decimais") BigDecimal valor_base,

                        @Null(message = "created_by é preenchido automaticamente pelo usuário autenticado") UUID created_by) {
        }

        public record ServiceResponse(
                        UUID id,
                        String nome,
                        String descricao,
                        String categoria,
                        BillingType tipoCobranca,
                        BigDecimal valorBase,
                        UUID createdBy,
                        UUID updatedBy,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        }

        public record ServicePageResponse(
                        List<ServiceResponse> items,
                        long total,
                        int page,
                        int limit,
                        int totalPages) {
        }

        public record DeleteServiceResponse(String message) {
        }
}