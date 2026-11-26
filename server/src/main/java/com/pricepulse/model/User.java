package com.pricepulse.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A Price Pulse account (SDS 3.1, Figure 2).
 *
 * Inheritance: Shopper and Admin extend this class. All of them are stored in the ONE table "users"
 * (single-table inheritance). The "role" column says which subclass a row is, so Hibernate builds a
 * Shopper for role = SHOPPER and an Admin for role = ADMIN.
 *
 * Polymorphism: code that holds a User reference calls getDashboardPath() or canReviewObservations()
 * without knowing the subclass; Java runs the overriding method of the real object.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** Only the BCrypt hash is stored, never the password (FR-03). */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Needed by JPA to build objects from database rows. Not for application code. */
    protected User() {
    }

    protected User(String name, String email, String passwordHash) {
        this.name = ModelChecks.requireText(name, "name");
        this.email = ModelChecks.requireText(email, "email");
        this.passwordHash = ModelChecks.requireText(passwordHash, "passwordHash");
        this.createdAt = Instant.now();
    }

    /** The role this subclass stands for (matches the discriminator value). */
    public abstract Role getRole();

    /** Where the web dashboard sends this user after login (SDS 3.1). */
    public abstract String getDashboardPath();

    /** Whether this user may approve or reject flagged observations (FR-37). */
    public abstract boolean canReviewObservations();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
