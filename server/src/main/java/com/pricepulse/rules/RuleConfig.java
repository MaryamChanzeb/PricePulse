package com.pricepulse.rules;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Every rule threshold, read from application.properties (keys "pricepulse.rules.*").
 * SDS 4.2: the thresholds live in configuration and are passed to the rules through this object,
 * so they can be tuned while testing without touching Java code (NFR-07).
 * A record is immutable: nobody can change a threshold while the server runs.
 */
@ConfigurationProperties(prefix = "pricepulse.rules")
public record RuleConfig(
        int flagChangePercent,          // FR-13: 60
        int duplicateWindowHours,       // FR-12: 6
        BigDecimal maxPrice,            // FR-10: 10,000,000
        int minObservations,            // FR-15: 3
        int minHistoryDays,             // FR-15: 7
        int inflatedLookbackDays,       // FR-16: 60
        int inflatedMarginPercent,      // FR-16: 10
        int hikePercent,                // FR-17: 15
        int hikeWindowDays,             // FR-17: 14
        int perpetualLookbackDays,      // FR-18: 30
        int perpetualMinObservations,   // FR-18: 5
        int perpetualRatioPercent,      // FR-18: 80
        int medianDays,                 // FR-21: 30
        int watchlistLimit) {           // FR-26: 20
}
