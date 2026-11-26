package com.pricepulse.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Body of POST /api/watchlist (FR-24). */
public record WatchRequest(
        @NotNull(message = "Product is required")
        Long productId,

        @NotNull(message = "Target price is required")
        @Positive(message = "Target price must be greater than zero")
        @DecimalMax(value = "10000000", message = "Target price is too high")
        BigDecimal targetPrice) {
}
