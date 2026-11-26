package com.pricepulse.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Data for the price history chart (FR-34, wireframe Screen 3): accepted observations only, oldest first,
 * plus "Observations used: N accepted, M flagged" and "Last seen".
 */
public record ProductHistoryResponse(
        Long productId,
        String title,
        String storeName,
        String url,
        String imageUrl,
        int days,
        List<Point> points,
        long acceptedCount,
        long flaggedCount,
        Instant lastSeen) {

    public record Point(BigDecimal price, BigDecimal claimedOriginalPrice, boolean discountBadgeShown, Instant observedAt) {
    }
}
