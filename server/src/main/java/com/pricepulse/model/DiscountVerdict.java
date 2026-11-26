package com.pricepulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The verdict produced by one observation, with its reason and discount percentages (FR-21, FR-23).
 * A verdict is a record of what the history showed at that moment, so it is never edited: no setters,
 * no state-changing methods.
 */
@Entity
@Table(name = "discount_verdicts")
public class DiscountVerdict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "observation_id", nullable = false, unique = true)
    private PriceObservation observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Verdict verdict;

    /** Null when there is not enough history to compute a median. */
    @Column(name = "real_discount_pct", precision = 7, scale = 2)
    private BigDecimal realDiscountPct;

    /** Null when the page showed no claimed original price. */
    @Column(name = "claimed_discount_pct", precision = 7, scale = 2)
    private BigDecimal claimedDiscountPct;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DiscountVerdict() {
    }

    public DiscountVerdict(Product product, PriceObservation observation, Verdict verdict,
                           BigDecimal realDiscountPct, BigDecimal claimedDiscountPct, String reason) {
        this.product = ModelChecks.requireValue(product, "product");
        this.observation = ModelChecks.requireValue(observation, "observation");
        this.verdict = ModelChecks.requireValue(verdict, "verdict");
        this.realDiscountPct = realDiscountPct;
        this.claimedDiscountPct = claimedDiscountPct;
        this.reason = ModelChecks.requireText(reason, "reason");
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public PriceObservation getObservation() {
        return observation;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public BigDecimal getRealDiscountPct() {
        return realDiscountPct;
    }

    public BigDecimal getClaimedDiscountPct() {
        return claimedDiscountPct;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
