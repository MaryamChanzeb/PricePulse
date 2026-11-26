-- =====================================================================
-- Price Pulse base data. Run after schema.sql.
-- No users here: the Admin account is created by the server at startup
-- from the ADMIN_EMAIL and ADMIN_PASSWORD environment variables, so no
-- password or hash is ever stored in the repository (SRS 2.2, NFR-02).
-- =====================================================================

INSERT INTO stores (name, domain, currency, is_supported) VALUES
    ('Daraz',                                 'daraz.pk',       'PKR', TRUE),
    ('Demo Store (class demonstration only)', 'localhost:8080', 'PKR', TRUE),
    ('Example store',                         'example.pk',     'PKR', FALSE);
