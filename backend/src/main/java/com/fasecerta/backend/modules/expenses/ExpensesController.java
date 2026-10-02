package com.fasecerta.backend.modules.expenses;

import com.fasecerta.backend.shared.enums.CategoryExpenses;
import com.fasecerta.backend.shared.enums.PaymentMethod;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/despesas")
public class ExpensesController {

    private final ExpensesService expensesService;

    public ExpensesController(ExpensesService expensesService) {
        this.expensesService = expensesService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpensesEntity> create(
            @Valid @RequestBody ExpensesDtos.CreateExpenseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(expensesService.create(request, authentication));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpensesDtos.ExpensePageResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) CategoryExpenses categoria,
            @RequestParam(required = false) Boolean pago,
            @RequestParam(name = "tipo_pagamento", required = false) ExpensePaymentType tipoPagamento,
            @RequestParam(name = "modo_pagamento", required = false) PaymentMethod modoPagamento,
            @RequestParam(name = "data_inicial", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(name = "data_final", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal
    ) {
        return ResponseEntity.ok(expensesService.list(
                page,
                limit,
                categoria,
                pago,
                tipoPagamento,
                modoPagamento,
                dataInicial,
                dataFinal
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpensesEntity> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(expensesService.findById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpensesEntity> update(
            @PathVariable UUID id,
            @Valid @RequestBody ExpensesDtos.UpdateExpenseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(expensesService.update(id, request, authentication));
    }
    

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and (hasAuthority('ADMIN') or hasRole('ADMIN'))")
    public ResponseEntity<ExpensesDtos.DeleteExpenseResponse> remove(
            @PathVariable UUID id,

            Authentication authentication
    ) {
        expensesService.remove(id, authentication);
        return ResponseEntity.ok(new ExpensesDtos.DeleteExpenseResponse("Despesa removida com sucesso"));
    }
}
