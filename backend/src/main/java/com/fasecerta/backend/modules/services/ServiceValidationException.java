package com.fasecerta.backend.modules.services;

public class ServiceValidationException extends RuntimeException {

    public ServiceValidationException(String message) {
        super(message);
    }
}