package com.pricepulse.rules;

import com.pricepulse.model.Verdict;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * FR-17 / SDS 4.2: the store raised the price by at least 15% in the 14 days before the sale price
 * appeared, and the "sale" price is not lower than the price before the rise.
 *
 * Steps:
 *  1. Sale start = when the current price first appeared (walk back while the price stays the same).
 *  2. Window = the 14 days before the sale start. Peak = highest price inside the window.
 *  3. Base = the last price before the window (the price "before the rise"); if there is none,
 *     the oldest price inside the window.
 *  4. Problem if peak is at least 15% above base AND the sale price is not lower than base.
 */
@Component
public class PreSaleHikeRule extends DiscountRule {

    public PreSaleHikeRule(RuleConfig config) {
        super(config);
    }

    @Override
    public Optional<Finding> evaluate(List<PricePoint> history, PricePoint snapshot) {
        if (!snapshot.showsDiscount()) {
            return Optional.empty(); // only a page that shows a sale can be a "raise then reduce" trick
        }
        BigDecimal salePrice = snapshot.price();

        // 1. When did the current price appear? History is newest first.
        Instant saleStart = snapshot.observedAt();
        for (PricePoint point : history) {
            if (point.observedAt().isAfter(snapshot.observedAt())) {
                continue;
            }
            if (point.price().compareTo(salePrice) != 0) {
                break;
            }
            if (point.observedAt().isBefore(saleStart)) {
                saleStart = point.observedAt();
            }
        }

        // 2 and 3. Peak inside the window, base just before it.
        Instant windowStart = saleStart.minus(Duration.ofDays(config.hikeWindowDays()));
        BigDecimal peak = null;
        BigDecimal oldestInWindow = null;
        BigDecimal base = null;
        for (PricePoint point : history) {
            Instant time = point.observedAt();
            if (!time.isBefore(saleStart)) {
                continue; // the sale itself
            }
            if (!time.isBefore(windowStart)) {
                if (peak == null || point.price().compareTo(peak) > 0) {
                    peak = point.price();
                }
                oldestInWindow = point.price(); // newest first, so the last one seen is the oldest
            } else {
                base = point.price(); // first point older than the window
                break;
            }
        }
        if (peak == null) {
            return Optional.empty(); // no prices in the 14 days before the sale
        }
        if (base == null) {
            base = oldestInWindow;
        }

        // 4. rise >= hike%   <=>   (peak - base) * 100 >= base * hike%   (exact)
        boolean roseEnough = peak.subtract(base).multiply(Prices.HUNDRED)
                .compareTo(base.multiply(BigDecimal.valueOf(config.hikePercent()))) >= 0;
        boolean notBelowBefore = salePrice.compareTo(base) >= 0;
        if (!roseEnough || !notBelowBefore) {
            return Optional.empty();
        }
        String reason = "The price was raised from " + Prices.rs(base) + " to " + Prices.rs(peak)
                + " in the " + config.hikeWindowDays() + " days before this sale, and the sale price "
                + Prices.rs(salePrice) + " is not lower than before the rise.";
        return Optional.of(new Finding(Verdict.PRICE_HIKE_BEFORE_SALE, reason));
    }
}
