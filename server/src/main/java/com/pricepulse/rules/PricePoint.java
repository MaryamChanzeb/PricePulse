package com.pricepulse.rules;

import com.pricepulse.model.PriceObservation;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The part of an observation the rules need. The rules work on these small read-only records instead of
 * entities, so they can be tested with prepared price histories and no database (NFR-08).
 */
public record PricePoint(BigDecimal price, BigDecimal claimedOriginalPrice, boolean discountBadgeShown, Instant observedAt) {

    public static PricePoint from(PriceObservation observation) {
        return new PricePoint(observation.getPrice(), observation.getClaimedOriginalPrice(),
                observation.isDiscountBadgeShown(), observation.getObservedAt());
    }

    /** True when the page showed a sale: a crossed-out price or a discount badge. */
    public boolean showsDiscount() {
        return claimedOriginalPrice != null || discountBadgeShown;
    }
}
