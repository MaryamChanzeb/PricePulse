package com.pricepulse.rules;

import com.pricepulse.model.Verdict;

/** A problem one rule detected: which verdict it means and a one-sentence reason in plain language (FR-21). */
public record Finding(Verdict verdict, String reason) {
}
