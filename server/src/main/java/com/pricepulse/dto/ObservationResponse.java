package com.pricepulse.dto;

import com.pricepulse.model.ObservationStatus;

/**
 * Answer to POST /api/observations.
 * duplicate = true means the same user sent this product within the duplicate window, so nothing was saved (FR-12);
 * the verdict is then the existing latest verdict of the product.
 */
public record ObservationResponse(
        Long observationId,
        Long productId,
        String productTitle,
        ObservationStatus status,
        boolean duplicate,
        String message,
        VerdictResponse verdict) {
}
