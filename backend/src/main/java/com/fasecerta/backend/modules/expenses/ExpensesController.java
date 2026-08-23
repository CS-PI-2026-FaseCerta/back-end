package com.fasecerta.backend.modules.expenses;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
