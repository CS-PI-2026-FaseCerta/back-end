package com.fasecerta.backend.exceptions;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.fasecerta.backend.modules.customer.CustomerConflictException;
import com.fasecerta.backend.modules.customer.CustomerValidationException;
import com.fasecerta.backend.modules.services.ServiceValidationException;

@RestControllerAdvice
public class GlobalExceptions {

        @ExceptionHandler(CustomerConflictException.class)
        public ResponseEntity<Map<String, String>> handleConflict(
                        CustomerConflictException exception) {

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "message",
                                                exception.getMessage()));
        }

        @ExceptionHandler(CustomerValidationException.class)
        public ResponseEntity<Map<String, String>> handleCustomerValidation(
                        CustomerValidationException exception) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "message",
                                                exception.getMessage()));
        }

        @ExceptionHandler(ServiceValidationException.class)
        public ResponseEntity<Map<String, String>> handleServiceValidation(
                        ServiceValidationException exception) {

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "message",
                                                exception.getMessage()));
        }

        @ExceptionHandler(UnauthenticatedException.class)
        public ResponseEntity<Map<String, String>> handleUnauthenticated(
                        UnauthenticatedException exception) {

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of(
                                                "message",
                                                exception.getMessage()));
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<Map<String, String>> handleResponseStatus(
                        ResponseStatusException exception) {

                return ResponseEntity
                                .status(exception.getStatusCode())
                                .body(Map.of(
                                                "message",
                                                exception.getReason() != null ? exception.getReason()
                                                                : "Erro no servidor"));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationErrors(
                        MethodArgumentNotValidException exception) {

                String message = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "message",
                                                message));
        }
}