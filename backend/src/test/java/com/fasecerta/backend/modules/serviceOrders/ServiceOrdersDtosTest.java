package com.fasecerta.backend.modules.serviceOrders;

import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.CreateServiceOrderRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemRequest;
import com.fasecerta.backend.modules.serviceOrders.ServiceOrdersDtos.ServiceItemResponse;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        var item = item(1L, null);
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
        var request = request(List.of(item(0L, null)));

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void acceptsAtMostFifteenQuantityDigits() {
        assertTrue(VALIDATOR.validate(request(List.of(item(999_999_999_999_999L, null)))).isEmpty());
        assertFalse(VALIDATOR.validate(request(List.of(item(1_000_000_000_000_000L, null)))).isEmpty());
    }

    @Test
    void rejectsNegativeNegotiatedPrice() {
        var request = request(List.of(
                item(1L, new BigDecimal("-0.01"))));

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void preservesRecordedFractionalQuantitiesInHistoricalResponses() {
        var response = new ServiceItemResponse(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1.2500"), new BigDecimal("20.00"), new BigDecimal("25.00"), null);

        assertEquals(new BigDecimal("1.2500"), response.quantidade());
    }

    private CreateServiceOrderRequest request(List<ServiceItemRequest> items) {
        return new CreateServiceOrderRequest(
                UUID.randomUUID(),
                items,
                null,
                null);
    }

    private ServiceItemRequest item(
            long quantity,
            BigDecimal negotiatedPrice) {

        return new ServiceItemRequest(
                UUID.randomUUID(),
                quantity,
                negotiatedPrice,
                null);
    }
}
