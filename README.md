# Price Pulse

A Chrome extension and web dashboard that tells online shoppers in Pakistan whether a store discount is real, and alerts them when a product reaches their target price.

SEL-311 Software Construction, Lab Group Project, Bahria University Islamabad (H11), BSE-5 Morning.
Instructor: Engr. Saad Mazhar Khan

## Members
- Maryam Chanzeb (09-131242-044)
- Zahra Latif (09-131242-092)

## What it does
- Reads the price on a supported store product page (first store: Daraz) and shows a verdict: genuine, inflated original price, price raised before sale, permanent sale, or not enough data.
- Keeps a price history and lets shoppers watch products with a target price.
- Sends live notifications when a price reaches the target.
- Admin panel for reviewing suspicious prices and managing supported stores.

## Technology
Java 17, Spring Boot, MySQL 8, HTML/CSS/JavaScript with Bootstrap, Chrome Extension (Manifest V3), JUnit 5, Mockito.

## Folder structure
- `server/` Spring Boot application (controller, service, repository, model, rules, exception, security)
- `extension/` Chrome extension (manifest.json, background.js, content.js, adapters/, popup)
- `database/` schema.sql and seed.sql
- `docs/` proposal, SRS, SDS and wireframes

## How to install and run
(To be completed as the project is built.)
1. Create the MySQL database using `database/schema.sql` and `database/seed.sql`.
2. Copy `server/src/main/resources/application-example.properties` to `application-local.properties` (same folder) and fill in your own values. Never commit this file.
3. Run the server from `server/` and open the dashboard.
4. In Chrome open `chrome://extensions`, enable Developer mode, choose "Load unpacked" and select the `extension/` folder.

## Screenshots
(To be added.)
