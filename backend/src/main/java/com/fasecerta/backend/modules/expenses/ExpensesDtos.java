package com.fasecerta.backend.modules.expenses;

import com.fasecerta.backend.shared.enums.CategoryExpenses;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ExpensesDtos {

    private ExpensesDtos() {
    }

    public record CreateExpenseRequest(
            @NotNull(message = "data é obrigatória")
            LocalDate data,

            @NotBlank(message = "descricao é obrigatória")
            String descricao,

            @NotBlank(message = "pago_a é obrigatório")
            String pago_a,

            @NotNull(message = "categoria é obrigatória")
            CategoryExpenses categoria,

            @NotNull(message = "valor é obrigatório")
            @DecimalMin(value = "0", inclusive = false, message = "valor deve ser maior que zero")
            @Digits(integer = 17, fraction = 2, message = "valor deve possuir no máximo duas casas decimais")
            BigDecimal valor,

            @NotNull(message = "tipo_pagamento é obrigatório")
            ExpensePaymentType tipo_pagamento,

            @NotNull(message = "modo_pagamento é obrigatório")
            PaymentMethod modo_pagamento,

            Boolean pago,

            @Null(message = "created_by é preenchido automaticamente pelo usuário autenticado")
            UUID created_by
    ) {
    }

    public record ExpensePageResponse(
            List<ExpensesEntity> items,
            long total,
            int page,
            int limit,
            int totalPages
    ) {
    }
}
