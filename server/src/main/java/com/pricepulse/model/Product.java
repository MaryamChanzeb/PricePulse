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

import java.time.Instant;

/** One product of one store, identified by the store's own SKU. Created the first time it is seen (FR-14). */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "store_sku", nullable = false, length = 100)
    private String storeSku;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Product() {
    }

    public Product(Store store, String storeSku, String title, String url, String imageUrl) {
        this.store = ModelChecks.requireValue(store, "store");
        this.storeSku = ModelChecks.requireText(storeSku, "storeSku");
        this.title = ModelChecks.requireText(title, "title");
        this.url = ModelChecks.requireText(url, "url");
        this.imageUrl = (imageUrl == null || imageUrl.isBlank()) ? null : imageUrl.trim();
        this.createdAt = Instant.now();
    }

    /**
     * Stores rename products and change image links. When a later observation carries newer details,
     * the title, url and image are refreshed; the store and SKU (the identity) never change.
     */
    public void refreshDetails(String newTitle, String newUrl, String newImageUrl) {
        if (newTitle != null && !newTitle.isBlank()) {
            this.title = newTitle.trim();
        }
        if (newUrl != null && !newUrl.isBlank()) {
            this.url = newUrl.trim();
        }
        if (newImageUrl != null && !newImageUrl.isBlank()) {
            this.imageUrl = newImageUrl.trim();
        }
    }

    public Long getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public String getStoreSku() {
        return storeSku;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
