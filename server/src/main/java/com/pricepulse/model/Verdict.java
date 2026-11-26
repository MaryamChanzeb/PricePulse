package com.pricepulse.model;

/**
 * The possible results of the fake discount analysis (SRS 1.3).
 * priority: when several rules report a problem, the highest priority wins (FR-20).
 * Verdicts that are not rule findings (GENUINE, NOT_ENOUGH_DATA) have priority 0.
 */
public enum Verdict {
    GENUINE(0),
    INFLATED_ORIGINAL(3),
    PRICE_HIKE_BEFORE_SALE(2),
    PERPETUAL_SALE(1),
    NOT_ENOUGH_DATA(0);

    private final int priority;

    Verdict(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
