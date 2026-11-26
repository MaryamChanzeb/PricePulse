package com.pricepulse.dto;

import com.pricepulse.model.DiscountVerdict;
import com.pricepulse.model.Verdict;

import java.math.BigDecimal;
import java.time.Instant;

/** A verdict as JSON (FR-21): the word, a plain reason, the claimed and the real discount percent. */
public record VerdictResponse(
        Long productId,
        Verdict verdict,
        String reason,
        BigDecimal realDiscountPct,
        BigDecimal claimedDiscountPct,
        Instant createdAt) {

    public static VerdictResponse from(DiscountVerdict saved) {
        return new VerdictResponse(saved.getProduct().getId(), saved.getVerdict(), saved.getReason(),
                saved.getRealDiscountPct(), saved.getClaimedDiscountPct(), saved.getCreatedAt());
    }

    /** For a product that has never been analysed (for example its only price is FLAGGED). */
    public static VerdictResponse notAnalysedYet(Long productId) {
        return new VerdictResponse(productId, Verdict.NOT_ENOUGH_DATA,
                "No price has been analysed for this product yet.", null, null, null);
    }
}
