-- =====================================================================
-- Price Pulse database schema (MySQL 8)            SDS section 5
-- Run in MySQL Workbench with the `pricepulse` schema selected.
-- This script DROPS and re-creates all tables: it deletes existing data.
-- Order: drop children first, create parents first (foreign keys).
-- =====================================================================

DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS watchlist_items;
DROP TABLE IF EXISTS discount_verdicts;
DROP TABLE IF EXISTS price_observations;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS stores;
DROP TABLE IF EXISTS users;

-- Accounts. Role decides the Java subclass (single-table inheritance): SHOPPER -> Shopper, ADMIN -> Admin.
-- Only the BCrypt hash is stored, never the password (FR-03).
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('SHOPPER', 'ADMIN'))
);

-- Supported stores. An Admin can switch a store off with is_supported (FR-38).
-- domain is the page host without "www." (plus ":port" for local demo pages), e.g. daraz.pk
CREATE TABLE stores (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    domain       VARCHAR(255) NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    is_supported BOOLEAN      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stores_domain UNIQUE (domain)
);

-- One row per store product. Created the first time its SKU is seen (FR-14).
CREATE TABLE products (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    store_id   BIGINT        NOT NULL,
    store_sku  VARCHAR(100)  NOT NULL,
    title      VARCHAR(500)  NOT NULL,
    url        VARCHAR(1000) NOT NULL,
    image_url  VARCHAR(1000) NULL,
    created_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_store_sku UNIQUE (store_id, store_sku),
    CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

-- Every recorded price. Never edited except its status (ACCEPTED, FLAGGED, REJECTED) - FR-13, FR-37.
CREATE TABLE price_observations (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    product_id             BIGINT        NOT NULL,
    user_id                BIGINT        NOT NULL,
    price                  DECIMAL(12,2) NOT NULL,
    claimed_original_price DECIMAL(12,2) NULL,
    discount_badge_shown   BOOLEAN       NOT NULL,
    status                 VARCHAR(20)   NOT NULL,
    observed_at            DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_observations_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_observations_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT ck_observations_status  CHECK (status IN ('ACCEPTED', 'FLAGGED', 'REJECTED')),
    CONSTRAINT ck_observations_price   CHECK (price > 0)
);
-- The rules read the history of one product ordered by time.
CREATE INDEX ix_observations_product_time ON price_observations (product_id, observed_at);

-- The verdict produced by one observation (FR-23). UNIQUE observation_id = "one observation produces one verdict".
CREATE TABLE discount_verdicts (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    product_id           BIGINT       NOT NULL,
    observation_id       BIGINT       NOT NULL,
    verdict              VARCHAR(30)  NOT NULL,
    real_discount_pct    DECIMAL(7,2) NULL,
    claimed_discount_pct DECIMAL(7,2) NULL,
    reason               VARCHAR(500) NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_verdicts_observation UNIQUE (observation_id),
    CONSTRAINT fk_verdicts_product     FOREIGN KEY (product_id)     REFERENCES products (id),
    CONSTRAINT fk_verdicts_observation FOREIGN KEY (observation_id) REFERENCES price_observations (id)
);
CREATE INDEX ix_verdicts_product_time ON discount_verdicts (product_id, created_at);

-- A shopper's watched product (FR-24 to FR-29). UNIQUE (user_id, product_id) = FR-25 also enforced by the database.
CREATE TABLE watchlist_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    target_price DECIMAL(12,2) NOT NULL,
    alert_armed  BOOLEAN       NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_watchlist_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_watchlist_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_watchlist_target  CHECK (target_price > 0)
);

-- Messages for a user about a product (FR-28, FR-30 to FR-32).
CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    product_id BIGINT       NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_notifications_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);
-- The extension asks for the unread count every minute (FR-31).
CREATE INDEX ix_notifications_user_read ON notifications (user_id, is_read);
