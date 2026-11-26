package com.pricepulse.rules;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** FR-28: the new price is at or below the shopper's target. Checked first (@Order 1). */
@Component
@Order(1)
public class TargetPriceAlertRule extends AlertRule {

    @Override
    public boolean isTriggered(BigDecimal previous, BigDecimal current, BigDecimal target) {
        return current.compareTo(target) <= 0;
    }

    @Override
    public String message(String productTitle, BigDecimal previous, BigDecimal current, BigDecimal target) {
        return productTitle + " is now " + Prices.rs(current) + ", at or below your target of " + Prices.rs(target) + ".";
    }
}
