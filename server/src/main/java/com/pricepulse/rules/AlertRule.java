package com.pricepulse.rules;

import java.math.BigDecimal;

/**
 * Parent of the alert rules (SDS 3.1, Figure 4).
 * WatchlistService keeps a List of AlertRule and asks each one, through this parent type, whether a new
 * price should alert the shopper. Each subclass also writes its own message (polymorphism).
 */
public abstract class AlertRule {

    /**
     * @param previous the last accepted price before this one, or null if there was none
     * @param current  the new accepted price
     * @param target   the shopper's target price
     */
    public abstract boolean isTriggered(BigDecimal previous, BigDecimal current, BigDecimal target);

    /** The notification text shown to the shopper when this rule fires. */
    public abstract String message(String productTitle, BigDecimal previous, BigDecimal current, BigDecimal target);
}
