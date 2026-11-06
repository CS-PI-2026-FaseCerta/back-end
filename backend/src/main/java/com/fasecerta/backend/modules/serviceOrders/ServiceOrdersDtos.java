package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.shared.enums.DiscountType;
import com.fasecerta.backend.shared.enums.PaymentCondition;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import com.fasecerta.backend.shared.enums.StatusOS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ServiceOrdersDtos {

    private ServiceOrdersDtos() {
    }

    public record CreateServiceOrderRequest(
            @NotNull(message = "clienteId é obrigatório")
            UUID clienteId,

            @NotEmpty(message = "A OS deve possuir pelo menos um serviço")
            List<@NotNull @Valid ServiceItemRequest> servicos,

            @Valid
            DiscountRequest desconto,

            @Valid
            PaymentRequest pagamento) {
    }

    public record ServiceItemRequest(
            @NotNull(message = "servicoId é obrigatório")
            UUID servicoId,

            @NotNull(message = "quantidade é obrigatória")
            @DecimalMin(value = "0", inclusive = false, message = "quantidade deve ser maior que zero")
            BigDecimal quantidade,

            @DecimalMin(value = "0", message = "valorUnitario não pode ser negativo")
            @Digits(integer = 17, fraction = 2, message = "valorUnitario deve possuir no máximo duas casas decimais")
            BigDecimal valorUnitario,

            String observacao) {
    }

    public record DiscountRequest(
            @NotNull(message = "tipo do desconto é obrigatório")
            DiscountType tipo,

            @NotNull(message = "valor do desconto é obrigatório")
            @DecimalMin(value = "0", message = "valor do desconto não pode ser negativo")
            @Digits(integer = 17, fraction = 2, message = "valor do desconto deve possuir no máximo duas casas decimais")
            BigDecimal valor) {
    }

    public record PaymentRequest(
            PaymentCondition condicao,

            @DecimalMin(value = "0", message = "valorEntrada não pode ser negativo")
            @Digits(integer = 17, fraction = 2, message = "valorEntrada deve possuir no máximo duas casas decimais")
            BigDecimal valorEntrada,

            @Positive(message = "quantidadeParcelas deve ser maior que zero")
            Integer quantidadeParcelas,

            String descricaoParcelamento,

            String detalhes,

            List<@NotNull PaymentMethod> meios) {
    }

    public record ServiceOrderResponse(
            UUID id,
            Long numeroOs,
            UUID clienteId,
            StatusOS status,
            List<ServiceItemResponse> servicos,
            BigDecimal valorSubtotal,
            DiscountResponse desconto,
            BigDecimal valorTotal,
            PaymentResponse pagamento,
            LocalDateTime createdAt) {
    }

    public record ServiceItemResponse(
            UUID id,
            UUID servicoId,
            BigDecimal quantidade,
            BigDecimal valorUnitarioCobrado,
            BigDecimal valorTotalItem,
            String observacao) {
    }

    public record DiscountResponse(
            DiscountType tipo,
            BigDecimal valor,
            BigDecimal valorDescontoCalculado) {
    }

    public record PaymentResponse(
            PaymentCondition condicao,
            BigDecimal valorEntrada,
            Integer quantidadeParcelas,
            String descricaoParcelamento,
            String detalhes,
            List<PaymentMethod> meios) {
    }
}