package com.pricepulse.rules;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * SDS Figure 4: the price fell by at least dropPercent from the previous accepted price in one step,
 * even if it is still above the target. Checked after the target rule (@Order 2).
 */
@Component
@Order(2)
public class PercentageDropAlertRule extends AlertRule {

    private final int dropPercent;

    public PercentageDropAlertRule(RuleConfig config) {
        this.dropPercent = config.alertDropPercent();
    }

    @Override
    public boolean isTriggered(BigDecimal previous, BigDecimal current, BigDecimal target) {
        if (previous == null || current.compareTo(previous) >= 0) {
            return false; // no earlier price, or the price did not fall
        }
        // (previous - current) / previous >= drop%   <=>   (previous - current) * 100 >= previous * drop%   (exact)
        return previous.subtract(current).multiply(Prices.HUNDRED)
                .compareTo(previous.multiply(BigDecimal.valueOf(dropPercent))) >= 0;
    }

    @Override
    public String message(String productTitle, BigDecimal previous, BigDecimal current, BigDecimal target) {
        return productTitle + " dropped " + Prices.percentText(Prices.percentBelow(previous, current)) + " from "
                + Prices.rs(previous) + " to " + Prices.rs(current) + " (your target is " + Prices.rs(target) + ").";
    }

    public int getDropPercent() {
        return dropPercent;
    }
}
