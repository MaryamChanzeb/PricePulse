package com.pricepulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Locale;

/** A store that Price Pulse can read, for example Daraz (daraz.pk). FR-11, FR-38. */
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Page host without "www.", plus ":port" for local pages. Example: daraz.pk */
    @Column(nullable = false, unique = true, length = 255)
    private String domain;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "is_supported", nullable = false)
    private boolean supported;

    protected Store() {
    }

    public Store(String name, String domain, String currency, boolean supported) {
        this.name = ModelChecks.requireText(name, "name");
        this.domain = ModelChecks.requireText(domain, "domain").toLowerCase(Locale.ROOT);
        this.currency = ModelChecks.requireText(currency, "currency").toUpperCase(Locale.ROOT);
        this.supported = supported;
    }

    /** Admin switches the store on (FR-38). */
    public void enable() {
        this.supported = true;
    }

    /** Admin switches the store off; its observations are then rejected (FR-11, FR-38). */
    public void disable() {
        this.supported = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isSupported() {
        return supported;
    }
}
