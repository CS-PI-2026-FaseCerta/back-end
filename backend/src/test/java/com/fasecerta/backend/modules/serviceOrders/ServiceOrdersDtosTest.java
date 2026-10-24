package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.CreateServiceOrderRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceOrdersDtosTest {

    private static final ValidatorFactory FACTORY =
            Validation.buildDefaultValidatorFactory();

    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void acceptsRequestWithoutDiscountAndPayment() {
        var item = item(new BigDecimal("1.5"), null);
        var request = request(List.of(item));

        assertTrue(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void rejectsEmptyServices() {
        assertFalse(VALIDATOR.validate(request(List.of())).isEmpty());
    }

    @Test
    void rejectsNullServiceItem() {
        List<ServiceItemRequest> items =
                Arrays.asList((ServiceItemRequest) null);

        assertFalse(VALIDATOR.validate(request(items)).isEmpty());
    }

    @Test
    void rejectsZeroQuantity() {
        var request = request(List.of(item(BigDecimal.ZERO, null)));

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void rejectsNegativeNegotiatedPrice() {
        var request = request(List.of(
                item(BigDecimal.ONE, new BigDecimal("-0.01"))));

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    private CreateServiceOrderRequest request(List<ServiceItemRequest> items) {
        return new CreateServiceOrderRequest(
                UUID.randomUUID(),
                items,
                null,
                null);
    }

    private ServiceItemRequest item(
            BigDecimal quantity,
            BigDecimal negotiatedPrice) {

        return new ServiceItemRequest(
                UUID.randomUUID(),
                quantity,
                negotiatedPrice,
                null);
    }
}