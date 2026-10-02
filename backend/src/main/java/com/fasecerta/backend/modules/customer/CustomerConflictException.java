package com.fasecerta.backend.modules.customer;

public class CustomerConflictException extends RuntimeException {

    public CustomerConflictException(String message) {
        super(message);
    }
}