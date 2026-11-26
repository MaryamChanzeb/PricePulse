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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One recorded sighting of a product's price (SRS 1.3).
 *
 * Encapsulation: the price data never changes after creation, and the status changes ONLY through
 * markFlagged(), approve() and reject(). There is no setStatus(), so no code can, for example,
 * turn a REJECTED observation back into ACCEPTED by accident.
 */
@Entity
@Table(name = "price_observations")
public class PriceObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** The crossed-out "was" price; null when the page shows none. */
    @Column(name = "claimed_original_price", precision = 12, scale = 2)
    private BigDecimal claimedOriginalPrice;

    @Column(name = "discount_badge_shown", nullable = false)
    private boolean discountBadgeShown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ObservationStatus status;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    protected PriceObservation() {
    }

    /** A new observation starts as ACCEPTED; ObservationService flags it if it looks like an outlier. */
    public PriceObservation(Product product, User user, BigDecimal price, BigDecimal claimedOriginalPrice,
                            boolean discountBadgeShown, Instant observedAt) {
        this.product = ModelChecks.requireValue(product, "product");
        this.user = ModelChecks.requireValue(user, "user");
        this.price = ModelChecks.requirePositive(price, "price");
        this.claimedOriginalPrice = claimedOriginalPrice;
        this.discountBadgeShown = discountBadgeShown;
        this.observedAt = ModelChecks.requireValue(observedAt, "observedAt");
        this.status = ObservationStatus.ACCEPTED;
    }

    /** Holds a suspicious price for Admin review (FR-13). Only a fresh, accepted observation can be flagged. */
    public void markFlagged() {
        if (status != ObservationStatus.ACCEPTED) {
            throw new IllegalStateException("Only an accepted observation can be flagged, this one is " + status);
        }
        this.status = ObservationStatus.FLAGGED;
    }

    /** Admin approves a flagged price: it now counts in analysis (FR-37). */
    public void approve() {
        requireFlagged("approved");
        this.status = ObservationStatus.ACCEPTED;
    }

    /** Admin rejects a flagged price: it is excluded permanently (FR-37). */
    public void reject() {
        requireFlagged("rejected");
        this.status = ObservationStatus.REJECTED;
    }

    private void requireFlagged(String action) {
        if (status != ObservationStatus.FLAGGED) {
            throw new IllegalStateException("Only a flagged observation can be " + action + ", this one is " + status);
        }
    }

    public boolean isAccepted() {
        return status == ObservationStatus.ACCEPTED;
    }

    public boolean hasClaimedOriginalPrice() {
        return claimedOriginalPrice != null;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getClaimedOriginalPrice() {
        return claimedOriginalPrice;
    }

    public boolean isDiscountBadgeShown() {
        return discountBadgeShown;
    }

    public ObservationStatus getStatus() {
        return status;
    }

    public Instant getObservedAt() {
        return observedAt;
    }
}
