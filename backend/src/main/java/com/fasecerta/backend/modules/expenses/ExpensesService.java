package com.fasecerta.backend.modules.expenses;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ExpensesService {

    private final ExpensesRepository expensesRepository;

    public ExpensesService(ExpensesRepository expensesRepository) {
        this.expensesRepository = expensesRepository;
    }

    @Transactional
    public ExpensesEntity create(ExpensesDtos.CreateExpenseRequest request, Authentication authentication) {
        UUID createdBy = authenticatedUserId(authentication);

        ExpensesEntity expense = new ExpensesEntity();
        expense.setData(request.data());
        expense.setDescricao(request.descricao());
        expense.setPagoA(request.pago_a());
        expense.setCategoria(request.categoria());
        expense.setValor(request.valor());
        expense.setTipoPagamento(request.tipo_pagamento());
        expense.setModoPagamento(request.modo_pagamento());
        expense.setPago(Boolean.TRUE.equals(request.pago()));
        expense.setCreatedBy(createdBy);

        return expensesRepository.save(expense);
    }

    private UUID authenticatedUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "O usuário autenticado não possui um UUID válido"
            );
        }
    }
}
