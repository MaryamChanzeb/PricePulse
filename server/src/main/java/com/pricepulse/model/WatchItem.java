package com.pricepulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A product on a shopper's watchlist with a target price (FR-24 to FR-29).
 *
 * "Armed" means: the next time an accepted price is at or below the target, send ONE alert.
 * After the alert the item is disarmed, so the shopper is not alerted again for the same drop (FR-28).
 * When the price goes back above the target the item is armed again (FR-29).
 */
@Entity
@Table(name = "watchlist_items")
public class WatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "target_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "alert_armed", nullable = false)
    private boolean alertArmed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WatchItem() {
    }

    /** A new watch item starts armed (FR-24: target must be greater than zero). */
    public WatchItem(User user, Product product, BigDecimal targetPrice) {
        this.user = ModelChecks.requireValue(user, "user");
        this.product = ModelChecks.requireValue(product, "product");
        this.targetPrice = ModelChecks.requirePositive(targetPrice, "targetPrice");
        this.alertArmed = true;
        this.createdAt = Instant.now();
    }

    /** Ready to alert again (FR-29). */
    public void arm() {
        this.alertArmed = true;
    }

    /** An alert was sent for this drop; stay quiet until the price goes back above the target (FR-28). */
    public void disarm() {
        this.alertArmed = false;
    }

    /**
     * Shopper changes the target (FR-27). A new target is a new wish, so the item is armed again.
     */
    public void changeTarget(BigDecimal newTarget) {
        this.targetPrice = ModelChecks.requirePositive(newTarget, "targetPrice");
        this.alertArmed = true;
    }

    public boolean belongsTo(User someUser) {
        return someUser != null && user.getId() != null && user.getId().equals(someUser.getId());
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Product getProduct() {
        return product;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public boolean isAlertArmed() {
        return alertArmed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
