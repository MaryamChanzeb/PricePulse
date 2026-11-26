package com.pricepulse.model;

/** The two roles of SRS 2.2. Stored in users.role and used as the JPA discriminator. */
public enum Role {
    SHOPPER,
    ADMIN
}
