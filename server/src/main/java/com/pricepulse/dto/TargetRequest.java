package com.pricepulse.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Body of PUT /api/watchlist/{id}: the new target price (FR-27). */
public record TargetRequest(
        @NotNull(message = "Target price is required")
        @Positive(message = "Target price must be greater than zero")
        @DecimalMax(value = "10000000", message = "Target price is too high")
        BigDecimal targetPrice) {
}
