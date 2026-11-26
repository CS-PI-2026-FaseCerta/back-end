package com.fasecerta.backend.modules.serviceOrders;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import com.fasecerta.backend.shared.enums.DiscountType;
import com.fasecerta.backend.shared.enums.PaymentCondition;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import com.fasecerta.backend.shared.enums.StatusOS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.BeanProperty;

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
            @Positive(message = "quantidade deve ser maior que zero")
            @Max(value = ServiceItemRequest.MAX_QUANTITY, message = "quantidade aceita até 15 dígitos")
            @JsonDeserialize(using = ServiceQuantityDeserializer.class)
            Long quantidade,

            @DecimalMin(value = "0", message = "valorUnitario não pode ser negativo")
            @Digits(integer = 17, fraction = 2, message = "valorUnitario deve possuir no máximo duas casas decimais")
            BigDecimal valorUnitario,

            String observacao) {
            static final long MAX_QUANTITY = 999_999_999_999_999L;
        }

        public record DiscountRequest(
                @NotNull(message = "tipo do desconto é obrigatório")
                @JsonDeserialize(using = EnumPorNome.class)
                DiscountType tipo,

                @NotNull(message = "valor do desconto é obrigatório")
                @DecimalMin(value = "0", message = "valor do desconto não pode ser negativo")
                @Digits(integer = 17, fraction = 2, message = "valor do desconto deve possuir no máximo duas casas decimais")
                BigDecimal valor) {
        }

        public record PaymentRequest(
                @JsonDeserialize(using = EnumPorNome.class)
                PaymentCondition condicao,

                @DecimalMin(value = "0", message = "valorEntrada não pode ser negativo")
                @Digits(integer = 17, fraction = 2, message = "valorEntrada deve possuir no máximo duas casas decimais")
                BigDecimal valorEntrada,

                @Positive(message = "quantidadeParcelas deve ser maior que zero")
                @JsonDeserialize(using = ParcelasInteiras.class)
                Integer quantidadeParcelas,

                String descricaoParcelamento,

                String detalhes,

                @JsonDeserialize(contentUsing = EnumPorNome.class)
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

        public record ServiceOrderDetailResponse(
                @JsonUnwrapped ServiceOrderResponse order,
                String clienteNome,
                UUID createdBy,
                UUID updatedBy,
                LocalDateTime updatedAt) {
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

        public static final class ServiceQuantityDeserializer extends ValueDeserializer<Long> {
                @Override
                public Long deserialize(JsonParser parser, DeserializationContext context) {
                        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                                return context.reportInputMismatch(Long.class, "quantidade deve ser um número inteiro");
                        }
                        return parser.getLongValue();
                }
        }

        public static final class ParcelasInteiras extends ValueDeserializer<Integer> {

        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) {
                if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                        return context.reportInputMismatch(Integer.class, "quantidadeParcelas deve ser um número inteiro");
                }
                        return parser.getIntValue();
                }
        }

        public static final class EnumPorNome extends ValueDeserializer<Enum<?>> {

                private final Class<?> tipo;

                public EnumPorNome() {
                        this(null);
                }

                private EnumPorNome(Class<?> tipo) {
                        this.tipo = tipo;
                }

                @Override
                public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {

                        var tipoContextual = context.getContextualType();

                        if (tipoContextual == null) {
                                tipoContextual = property.getType();
                        }

                        if (tipoContextual.isContainerType()) {
                                tipoContextual = tipoContextual.getContentType();
                        }

                        return new EnumPorNome(tipoContextual.getRawClass());
                }

                @Override
                public Enum<?> deserialize(
                        JsonParser parser,
                        DeserializationContext context) {

                        if (parser.hasToken(JsonToken.VALUE_STRING)) {
                        String nome = parser.getString();

                        for (Object constante : tipo.getEnumConstants()) {
                                Enum<?> valor = (Enum<?>) constante;

                                if (valor.name().equals(nome)) {
                                return valor;
                                }
                        }
                        }

                        return context.reportInputMismatch(
                                tipo,
                                "Informe um nome válido para %s",
                                tipo.getSimpleName()
                        );
                }
        }
}
