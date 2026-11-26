package com.pricepulse.rules;

import com.pricepulse.model.Verdict;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * FR-16 / SDS 4.2: the crossed-out "was" price is more than 10% above the highest real price
 * of the last 60 days, so that price was never really charged.
 */
@Component
public class InflatedOriginalPriceRule extends DiscountRule {

    public InflatedOriginalPriceRule(RuleConfig config) {
        super(config);
    }

    @Override
    public Optional<Finding> evaluate(List<PricePoint> history, PricePoint snapshot) {
        BigDecimal claimed = snapshot.claimedOriginalPrice();
        if (claimed == null) {
            return Optional.empty(); // no "was" price shown, nothing to judge
        }

        Instant from = snapshot.observedAt().minus(Duration.ofDays(config.inflatedLookbackDays()));
        BigDecimal highest = null;
        for (PricePoint point : between(history, from, snapshot.observedAt())) {
            if (highest == null || point.price().compareTo(highest) > 0) {
                highest = point.price();
            }
        }
        if (highest == null) {
            return Optional.empty();
        }

        // claimed > highest * (100 + margin) / 100   <=>   claimed * 100 > highest * (100 + margin)   (exact)
        BigDecimal claimedTimes100 = claimed.multiply(Prices.HUNDRED);
        BigDecimal limitTimes100 = highest.multiply(BigDecimal.valueOf(100L + config.inflatedMarginPercent()));
        if (claimedTimes100.compareTo(limitTimes100) <= 0) {
            return Optional.empty();
        }
        String reason = "The claimed original price " + Prices.rs(claimed) + " is more than "
                + config.inflatedMarginPercent() + "% above the highest price seen in the last "
                + config.inflatedLookbackDays() + " days (" + Prices.rs(highest) + ").";
        return Optional.of(new Finding(Verdict.INFLATED_ORIGINAL, reason));
    }
}
