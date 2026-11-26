package com.pricepulse;

import com.pricepulse.rules.RuleConfig;

import java.math.BigDecimal;

/** The same threshold values as application.properties, for tests that run without Spring. */
public final class TestRules {

    private TestRules() {
    }

    public static RuleConfig defaults() {
        return new RuleConfig(60, 6, new BigDecimal("10000000"), 3, 7, 60, 10, 15, 14, 30, 5, 80, 30, 20);
    }
}
