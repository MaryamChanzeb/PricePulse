package com.pricepulse.dto;

/**
 * What the extension reads from a product page (FR-07) and sends to POST /api/observations.
 * The prices are Strings on purpose: a value like "abc" must reach our own check and give
 * "Invalid price" (FR-10) instead of a JSON parse error. JSON numbers such as 5999 are accepted too.
 */
public record ObservationRequest(
        String url,
        String sku,
        String title,
        String imageUrl,
        String price,
        String claimedOriginalPrice,
        Boolean discountBadgeShown) {
}
