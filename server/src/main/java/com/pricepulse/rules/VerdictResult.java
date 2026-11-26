package com.pricepulse.rules;

import com.pricepulse.model.Verdict;

import java.math.BigDecimal;

/** The outcome of one analysis before it is saved (FR-21: verdict, reason, real and claimed discount). */
public record VerdictResult(Verdict verdict, String reason, BigDecimal realDiscountPct, BigDecimal claimedDiscountPct) {
}
