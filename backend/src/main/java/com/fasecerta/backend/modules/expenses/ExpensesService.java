package com.fasecerta.backend.modules.expenses;

import com.fasecerta.backend.shared.enums.CategoryExpenses;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ExpensesService {

    private static final int MAX_PAGE_SIZE = 100;

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

    @Transactional(readOnly = true)
    public ExpensesDtos.ExpensePageResponse list(
            int page,
            int limit,
            CategoryExpenses categoria,
            Boolean pago,
            ExpensePaymentType tipoPagamento,
            PaymentMethod modoPagamento,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        validatePagination(page, limit);

        Specification<ExpensesEntity> filters = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

        if (categoria != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("categoria"), categoria));
        }
        if (pago != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("pago"), pago));
        }
        if (tipoPagamento != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("tipoPagamento"), tipoPagamento));
        }
        if (modoPagamento != null) {
            filters = filters.and((root, query, cb) -> cb.equal(root.get("modoPagamento"), modoPagamento));
        }
        if (dataInicial != null) {
            filters = filters.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.<LocalDate>get("data"), dataInicial));
        }
        if (dataFinal != null) {
            filters = filters.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.<LocalDate>get("data"), dataFinal));
        }

        Sort sort = Sort.by(
                Sort.Order.desc("data"),
                Sort.Order.desc("id")
        );
        PageRequest pageable = PageRequest.of(page - 1, limit, sort);
        Page<ExpensesEntity> result = expensesRepository.findAll(filters, pageable);

        return new ExpensesDtos.ExpensePageResponse(
                result.getContent(),
                result.getTotalElements(),
                page,
                limit,
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ExpensesEntity findById(UUID id) {
        return expensesRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa não encontrada"));
    }

    @Transactional
    public ExpensesEntity update(
            UUID id,
            ExpensesDtos.UpdateExpenseRequest request,
            Authentication authentication
    ) {
        UUID updatedBy = authenticatedUserId(authentication);
        ExpensesEntity expense = expensesRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa não encontrada"));

        if (request.data() != null) {
            expense.setData(request.data());
        }
        if (request.descricao() != null) {
            expense.setDescricao(request.descricao());
        }
        if (request.pago_a() != null) {
            expense.setPagoA(request.pago_a());
        }
        if (request.categoria() != null) {
            expense.setCategoria(request.categoria());
        }
        if (request.valor() != null) {
            expense.setValor(request.valor());
        }
        if (request.tipo_pagamento() != null) {
            expense.setTipoPagamento(request.tipo_pagamento());
        }
        if (request.modo_pagamento() != null) {
            expense.setModoPagamento(request.modo_pagamento());
        }
        if (request.pago() != null) {
            expense.setPago(request.pago());
        }

        expense.setUpdatedBy(updatedBy);
        return expensesRepository.save(expense);
    }
    

    @Transactional
    public void remove(UUID id, Authentication authentication) {
        UUID updatedBy = authenticatedUserId(authentication);
        ExpensesEntity expense = expensesRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa não encontrada"));

        expense.setUpdatedBy(updatedBy);
        expense.setDeletedAt(LocalDateTime.now());
        expensesRepository.save(expense);
    }

    private void validatePagination(int page, int limit) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page deve ser maior ou igual a 1");
        }
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "limit deve estar entre 1 e " + MAX_PAGE_SIZE
            );
        }
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
