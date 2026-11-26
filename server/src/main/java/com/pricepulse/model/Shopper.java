package com.pricepulse.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A shopper: signs up publicly, sees verdicts, watches products (SRS 2.2).
 * The watchlist limit of SDS Figure 2 is not stored per user; it comes from
 * pricepulse.rules.watchlist-limit so it can be tuned without a database change.
 */
@Entity
@DiscriminatorValue("SHOPPER")
public class Shopper extends User {

    protected Shopper() {
    }

    public Shopper(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.SHOPPER;
    }

    @Override
    public String getDashboardPath() {
        return "/dashboard.html";
    }

    @Override
    public boolean canReviewObservations() {
        return false;
    }
}
