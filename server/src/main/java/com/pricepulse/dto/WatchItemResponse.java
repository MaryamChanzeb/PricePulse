package com.pricepulse.dto;

import com.pricepulse.model.Verdict;

import java.math.BigDecimal;
import java.time.Instant;

/** One watchlist card (FR-33, wireframe Screen 2): name, store, current price, target, latest verdict. */
public record WatchItemResponse(
        Long id,
        Long productId,
        String title,
        String storeName,
        String url,
        String imageUrl,
        BigDecimal currentPrice,
        BigDecimal targetPrice,
        boolean alertArmed,
        Verdict verdict,
        Instant createdAt) {
}
