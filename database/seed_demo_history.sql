-- =====================================================================
-- DEMO DATA - NOT REAL PRICES.
-- Sample price history for the five products of the local Demo Store
-- (http://localhost:8080/demo-store/), so that every verdict type can be
-- shown in class (SRS section 6, "Early data"). Run after seed.sql.
-- Dates are relative to the current UTC time (the server stores all times
-- in UTC), so the history is always "recent" whenever the script is run.
--
-- Expected verdict when the extension visits each demo page:
--   demo-genuine    -> GENUINE                 (page: Rs 3,499, was Rs 4,199)
--   demo-inflated   -> INFLATED_ORIGINAL       (page: Rs 5,999, was Rs 9,999)
--   demo-hike       -> PRICE_HIKE_BEFORE_SALE  (page: Rs 9,999, was Rs 11,499)
--   demo-perpetual  -> PERPETUAL_SALE          (page: Rs 2,100, was Rs 2,299)
--   demo-new        -> NOT_ENOUGH_DATA         (page: Rs 1,500)
-- =====================================================================

-- A placeholder account that owns the demo observations. Its password_hash
-- is not a BCrypt hash, so nobody can ever log in with it.
INSERT INTO users (name, email, password_hash, role, created_at)
VALUES ('DEMO DATA (not a real account)', 'demo-data@pricepulse.local', 'DISABLED-NO-LOGIN', 'SHOPPER', UTC_TIMESTAMP(6));
SET @demo_user = LAST_INSERT_ID();

SET @demo_store = (SELECT id FROM stores WHERE domain = 'localhost:8080');

INSERT INTO products (store_id, store_sku, title, url, image_url, created_at) VALUES
    (@demo_store, 'demo-genuine',   'DEMO Wireless Earbuds Pro', 'http://localhost:8080/demo-store/genuine.html',   NULL, UTC_TIMESTAMP(6) - INTERVAL 50 DAY),
    (@demo_store, 'demo-inflated',  'DEMO Smart Watch X2',       'http://localhost:8080/demo-store/inflated.html',  NULL, UTC_TIMESTAMP(6) - INTERVAL 50 DAY),
    (@demo_store, 'demo-hike',      'DEMO Air Fryer 5L',         'http://localhost:8080/demo-store/hike.html',      NULL, UTC_TIMESTAMP(6) - INTERVAL 50 DAY),
    (@demo_store, 'demo-perpetual', 'DEMO Backpack 30L',         'http://localhost:8080/demo-store/perpetual.html', NULL, UTC_TIMESTAMP(6) - INTERVAL 44 DAY),
    (@demo_store, 'demo-new',       'DEMO Desk Lamp',            'http://localhost:8080/demo-store/new.html',       NULL, UTC_TIMESTAMP(6) - INTERVAL 2 DAY);

SET @p_genuine   = (SELECT id FROM products WHERE store_id = @demo_store AND store_sku = 'demo-genuine');
SET @p_inflated  = (SELECT id FROM products WHERE store_id = @demo_store AND store_sku = 'demo-inflated');
SET @p_hike      = (SELECT id FROM products WHERE store_id = @demo_store AND store_sku = 'demo-hike');
SET @p_perpetual = (SELECT id FROM products WHERE store_id = @demo_store AND store_sku = 'demo-perpetual');
SET @p_new       = (SELECT id FROM products WHERE store_id = @demo_store AND store_sku = 'demo-new');

-- GENUINE: steady price around Rs 4,200, rarely on sale. Today's Rs 3,499 is a real ~17% drop.
INSERT INTO price_observations (product_id, user_id, price, claimed_original_price, discount_badge_shown, status, observed_at) VALUES
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 50 DAY),
    (@p_genuine, @demo_user, 4250.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 46 DAY),
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 42 DAY),
    (@p_genuine, @demo_user, 4299.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 38 DAY),
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 34 DAY),
    (@p_genuine, @demo_user, 4150.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 30 DAY),
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 26 DAY),
    (@p_genuine, @demo_user, 4250.00, 4500.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 22 DAY),
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 18 DAY),
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 14 DAY),
    (@p_genuine, @demo_user, 4150.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 10 DAY),
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 6 DAY),
    (@p_genuine, @demo_user, 4199.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 2 DAY);

-- INFLATED_ORIGINAL: never sold above Rs 6,299, but the page claims "was Rs 9,999" (more than 10% above).
INSERT INTO price_observations (product_id, user_id, price, claimed_original_price, discount_badge_shown, status, observed_at) VALUES
    (@p_inflated, @demo_user, 6199.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 50 DAY),
    (@p_inflated, @demo_user, 6299.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 45 DAY),
    (@p_inflated, @demo_user, 6249.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 40 DAY),
    (@p_inflated, @demo_user, 6199.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 35 DAY),
    (@p_inflated, @demo_user, 6299.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 30 DAY),
    (@p_inflated, @demo_user, 6249.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 25 DAY),
    (@p_inflated, @demo_user, 6199.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 20 DAY),
    (@p_inflated, @demo_user, 6249.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 15 DAY),
    (@p_inflated, @demo_user, 6299.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 10 DAY),
    (@p_inflated, @demo_user, 6249.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 5 DAY),
    (@p_inflated, @demo_user, 6249.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 2 DAY);

-- PRICE_HIKE_BEFORE_SALE: Rs ~9,500 for weeks, raised to ~Rs 11,500 twelve days ago,
-- then "reduced" to Rs 9,999 - still above the price before the rise.
INSERT INTO price_observations (product_id, user_id, price, claimed_original_price, discount_badge_shown, status, observed_at) VALUES
    (@p_hike, @demo_user,  9499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 50 DAY),
    (@p_hike, @demo_user,  9499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 45 DAY),
    (@p_hike, @demo_user,  9550.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 40 DAY),
    (@p_hike, @demo_user,  9499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 35 DAY),
    (@p_hike, @demo_user,  9499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 30 DAY),
    (@p_hike, @demo_user,  9499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 25 DAY),
    (@p_hike, @demo_user,  9450.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 20 DAY),
    (@p_hike, @demo_user,  9499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 16 DAY),
    (@p_hike, @demo_user, 11499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 12 DAY),
    (@p_hike, @demo_user, 11499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 9 DAY),
    (@p_hike, @demo_user, 11550.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 6 DAY),
    (@p_hike, @demo_user, 11499.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 3 DAY);

-- PERPETUAL_SALE: the "sale" badge is shown almost every time for 44 days (10 of 11).
INSERT INTO price_observations (product_id, user_id, price, claimed_original_price, discount_badge_shown, status, observed_at) VALUES
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 44 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 40 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 36 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 32 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 28 DAY),
    (@p_perpetual, @demo_user, 2150.00, NULL,    FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 24 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 20 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 16 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 12 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 8 DAY),
    (@p_perpetual, @demo_user, 2100.00, 2299.00, TRUE,  'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 4 DAY);

-- NOT_ENOUGH_DATA: only one earlier observation, two days ago.
INSERT INTO price_observations (product_id, user_id, price, claimed_original_price, discount_badge_shown, status, observed_at) VALUES
    (@p_new, @demo_user, 1500.00, NULL, FALSE, 'ACCEPTED', UTC_TIMESTAMP(6) - INTERVAL 2 DAY);
