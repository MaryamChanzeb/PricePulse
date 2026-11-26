package com.pricepulse.rules;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds prepared price histories for the rule tests, for example:
 *   History h = new History().at(20, "1000").at(10, "1200");
 *   PricePoint now = h.now("1000", "1200", true);   // also added to the history, like in the real service
 *   rule.evaluate(h.newestFirst(), now);
 */
final class History {

    static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

    private final List<PricePoint> points = new ArrayList<>();

    History at(int daysAgo, String price) {
        return at(daysAgo, price, null, false);
    }

    History at(int daysAgo, String price, String claimed, boolean badge) {
        points.add(new PricePoint(new BigDecimal(price), claimed == null ? null : new BigDecimal(claimed), badge,
                NOW.minus(Duration.ofDays(daysAgo))));
        return this;
    }

    /** Price "every N days" from daysAgo down to 1 day ago, all the same. */
    History every(int stepDays, int fromDaysAgo, String price, String claimed, boolean badge) {
        for (int day = fromDaysAgo; day >= 1; day -= stepDays) {
            at(day, price, claimed, badge);
        }
        return this;
    }

    /** The observation being judged, at NOW. It is added to the history too. */
    PricePoint now(String price, String claimed, boolean badge) {
        PricePoint snapshot = new PricePoint(new BigDecimal(price), claimed == null ? null : new BigDecimal(claimed),
                badge, NOW);
        points.add(snapshot);
        return snapshot;
    }

    /**
     * A LIVE view, newest first. In rule.evaluate(h.newestFirst(), h.now(...)) Java evaluates
     * newestFirst() BEFORE now(...) adds the snapshot; a copied list would miss it (bug B3).
     * The view is sorted each time it is read, so it always contains the snapshot.
     */
    List<PricePoint> newestFirst() {
        return new AbstractList<>() {
            @Override
            public PricePoint get(int index) {
                return sorted().get(index);
            }

            @Override
            public int size() {
                return points.size();
            }
        };
    }

    private List<PricePoint> sorted() {
        List<PricePoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparing(PricePoint::observedAt).reversed());
        return sorted;
    }
}
