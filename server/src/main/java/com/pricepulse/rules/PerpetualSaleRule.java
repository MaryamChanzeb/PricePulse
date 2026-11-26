package com.pricepulse.rules;

import com.pricepulse.model.Verdict;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * FR-18 / SDS 4.2: a discount badge was shown in at least 80% of at least 5 observations over the
 * last 30 days or more, so the "sale" is the normal state of the product.
 *
 * "Over the last 30 days or more" is read as: the product has been tracked for at least 30 days
 * (its oldest accepted observation is 30 or more days old), and the ratio is counted over the
 * observations of the last 30 days.
 */
@Component
public class PerpetualSaleRule extends DiscountRule {

    public PerpetualSaleRule(RuleConfig config) {
        super(config);
    }

    @Override
    public Optional<Finding> evaluate(List<PricePoint> history, PricePoint snapshot) {
        Instant from = snapshot.observedAt().minus(Duration.ofDays(config.perpetualLookbackDays()));

        Instant oldest = null;
        for (PricePoint point : history) {
            if (oldest == null || point.observedAt().isBefore(oldest)) {
                oldest = point.observedAt();
            }
        }
        if (oldest == null || oldest.isAfter(from)) {
            return Optional.empty(); // not tracked for the whole period yet
        }

        int total = 0;
        int withBadge = 0;
        for (PricePoint point : between(history, from, snapshot.observedAt())) {
            total++;
            if (point.discountBadgeShown()) {
                withBadge++;
            }
        }
        if (total < config.perpetualMinObservations()) {
            return Optional.empty();
        }

        // ratio >= 80%   <=>   withBadge * 100 >= total * 80   (exact, no rounding)
        if ((long) withBadge * 100 < (long) total * config.perpetualRatioPercent()) {
            return Optional.empty();
        }
        String reason = "A discount badge was shown in " + withBadge + " of " + total
                + " observations over the last " + config.perpetualLookbackDays()
                + " days, so this sale is the normal price.";
        return Optional.of(new Finding(Verdict.PERPETUAL_SALE, reason));
    }
}
