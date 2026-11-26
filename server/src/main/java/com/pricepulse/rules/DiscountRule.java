package com.pricepulse.rules;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parent of the three fake-discount rules (SDS 3.1, Figure 3).
 *
 * DiscountAnalysisService holds a List of DiscountRule and calls evaluate() on each element without knowing
 * which rule it is (polymorphism). Adding a fourth rule means writing one new subclass marked @Component;
 * Spring puts it into that list and the service does not change.
 */
public abstract class DiscountRule {

    /** Thresholds from application.properties (SDS Figure 3: "#config: RuleConfig"). */
    protected final RuleConfig config;

    protected DiscountRule(RuleConfig config) {
        this.config = config;
    }

    /**
     * @param history  accepted observations of the product, NEWEST FIRST, including the snapshot itself
     * @param snapshot the observation being judged (what the shopper sees now)
     * @return a Finding if this rule detects a problem, otherwise empty
     */
    public abstract Optional<Finding> evaluate(List<PricePoint> history, PricePoint snapshot);

    /** The points of the history from 'from' up to the snapshot time (newest first, like the input). */
    protected static List<PricePoint> between(List<PricePoint> history, Instant from, Instant until) {
        List<PricePoint> result = new ArrayList<>();
        for (PricePoint point : history) {
            if (!point.observedAt().isBefore(from) && !point.observedAt().isAfter(until)) {
                result.add(point);
            }
        }
        return result;
    }
}
