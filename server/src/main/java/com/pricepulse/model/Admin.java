package com.pricepulse.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * An admin: reviews flagged prices and manages stores (SRS 2.2).
 * Never created through public sign-up; the server creates it at startup from ADMIN_EMAIL and ADMIN_PASSWORD.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    protected Admin() {
    }

    public Admin(String name, String email, String passwordHash) {
        super(name, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getDashboardPath() {
        return "/admin.html";
    }

    @Override
    public boolean canReviewObservations() {
        return true;
    }
}
