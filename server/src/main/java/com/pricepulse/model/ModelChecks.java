package com.pricepulse.model;

import java.math.BigDecimal;

/**
 * Small guards used by the entity constructors so that an entity can never be built in an invalid state.
 * They throw IllegalArgumentException: by the time an entity is created, the services have already
 * checked user input and thrown a friendly custom exception, so reaching these means a programming error.
 */
final class ModelChecks {

    private ModelChecks() {
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }
}
