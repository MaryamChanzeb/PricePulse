# Test table

Only real results from real runs. "Actual" is what the run printed. Automated tests are JUnit 5 / Mockito / MockMvc
(`mvn test`); manual tests were HTTP calls with curl against the running server and MySQL 8.0.46.

| ID | FR | Type | Input | Expected | Actual | Result | Date |
|----|----|------|-------|----------|--------|--------|------|
| T01 | FR-01, FR-03 | curl | POST /api/auth/signup name "E2E Shopper", valid email, password "Shopper2026" | 201 with token; DB stores a BCrypt hash, not the password | 201 with token; users.password_hash starts with `$2a$10$`, length 60 | Pass | 26 Nov 2026 |
| T02 | FR-02 | curl | Sign-up again with the same email in different letter case | 409 "This email is already registered" | 409 "This email is already registered" | Pass | 26 Nov 2026 |
| T03 | FR-01 | curl | Sign-up with password "short" | 400 password rule message | 400 "Password must have at least 8 characters and at least one digit" | Pass | 26 Nov 2026 |
| T04 | FR-05 | curl | Login with correct email, wrong password | 401 "Invalid email or password" | 401 "Invalid email or password" | Pass | 26 Nov 2026 |
| T05 | FR-04, SDS 3.1 | curl | Login as the startup Admin | 200 with role ADMIN and dashboardPath /admin.html | 200, role ADMIN, dashboardPath /admin.html | Pass | 26 Nov 2026 |
| T06 | FR-06 | curl | GET /api/admin/flagged with a SHOPPER token | 403 "Access denied for your role" | 403 "Access denied for your role" | Pass | 26 Nov 2026 |
| T07 | NFR-02 | curl | GET /api/admin/flagged with no token | 401 | 401 "Please log in first" | Pass | 26 Nov 2026 |
| T08 | FR-05 | JUnit AuthServiceTest | Login with unknown email vs wrong password | Same message for both | Same message for both | Pass | 26 Nov 2026 |
| T09 | FR-04 | JUnit JwtServiceTest | Token checked 23 h and 25 h after it was made | Valid at 23 h, refused at 25 h | Valid at 23 h, refused at 25 h | Pass | 26 Nov 2026 |
| T10 | NFR-02 | JUnit JwtServiceTest | Token with changed characters / other secret | Refused | Refused | Pass | 26 Nov 2026 |
| T11 | FR-40 | MockMvc SecurityRulesTest | Login body "{oops" (broken JSON) | 400 JSON message, no stack trace | 400 "The request is not valid JSON" | Pass | 26 Nov 2026 |
| T12 | FR-40 | MockMvc SecurityRulesTest | Service throws an unexpected exception with an internal detail | 500 general message, detail hidden | 500 "Something went wrong, please try again" | Pass | 26 Nov 2026 |
| T13 | FR-13, FR-37 | JUnit PriceObservationTest | Reject a flagged observation, then try to approve it | Approve refused | IllegalStateException, status stays REJECTED | Pass | 26 Nov 2026 |
| T14 | FR-28, FR-29 | JUnit WatchItemTest | New item, disarm, arm | Armed, not armed, armed | Armed, not armed, armed | Pass | 26 Nov 2026 |
| T15 | FR-14, FR-25 | MySQL | Insert a second store with domain daraz.pk | Refused by the database | ERROR 1062 Duplicate entry 'daraz.pk' | Pass | 26 Nov 2026 |
| T16 | FR-10 | curl | POST /api/observations with price "abc" | 400 "Invalid price" | 400 "Invalid price" | Pass | 26 Nov 2026 |
| T17 | FR-10 | JUnit ObservationControllerTest | Prices 0, -5, "abc", "NaN", 10000000.01, empty | Each rejected with "Invalid price"; nothing saved | Each rejected, save never called | Pass | 26 Nov 2026 |
| T18 | FR-11 | curl | Observation from https://www.example.pk (store disabled) | 422 "This store is not supported" | 422 "This store is not supported" | Pass | 26 Nov 2026 |
| T19 | FR-12 | curl | Same shopper sends the demo earbuds twice within seconds | Second one not saved, answer marks duplicate | duplicate true, same observationId 49, "You already recorded this product in the last 6 hours" | Pass | 26 Nov 2026 |
| T20 | FR-13 | curl | Demo smart watch at Rs 599 (last accepted Rs 6,249) | Saved as FLAGGED | status FLAGGED, "held for review"; DB row status FLAGGED | Pass | 26 Nov 2026 |
| T21 | FR-13 | JUnit ObservationControllerTest | Last price 1000; new 1600, 400, 1600.01 | 60% exactly accepted, 60.001% flagged | First run: 1600.01 ACCEPTED (bug B2). After fix: ACCEPTED, ACCEPTED, FLAGGED | Pass after fix | 26 Nov 2026 |
| T22 | FR-14 | curl | Daraz page with a SKU never seen before | Product row created, observation ACCEPTED | New product id 6 created, ACCEPTED | Pass | 26 Nov 2026 |
| T23 | FR-01, FR-09 | Node + background.js against live server | SIGNUP, then LOGIN with wrong password, then LOGIN as admin | Account created; "Invalid email or password"; admin refused in extension | As expected; admin gets "Admin accounts use the web dashboard, not the extension." | Pass | 26 Nov 2026 |
| T24 | FR-19, FR-21 | curl, demo data | Demo earbuds Rs 3,499 (was Rs 4,199) | GENUINE with real discount vs 30-day median | GENUINE, real 16.67%, claimed 16.67%, "16.7% below the 30-day median of Rs 4,199" | Pass | 27 Nov 2026 |
| T25 | FR-16 | curl, demo data | Demo smart watch Rs 5,999 (was Rs 9,999) | INFLATED_ORIGINAL | INFLATED_ORIGINAL, claimed 40%, real 4%, highest of 60 days Rs 6,299 | Pass | 27 Nov 2026 |
| T26 | FR-17 | curl, demo data | Demo air fryer Rs 9,999 (was Rs 11,499) after a rise from Rs 9,499 | PRICE_HIKE_BEFORE_SALE | PRICE_HIKE_BEFORE_SALE, "raised from Rs 9,499 to Rs 11,550 in the 14 days before this sale" | Pass | 27 Nov 2026 |
| T27 | FR-18 | curl, demo data | Demo backpack, badge almost always shown | PERPETUAL_SALE | PERPETUAL_SALE, "badge shown in 7 of 8 observations over the last 30 days" | Pass | 27 Nov 2026 |
| T28 | FR-15 | curl, demo data | Demo desk lamp, 2 observations over 2 days | NOT_ENOUGH_DATA | NOT_ENOUGH_DATA, "2 observation(s) over 2 day(s)" | Pass | 27 Nov 2026 |
| T29 | FR-20 | JUnit DiscountAnalysisServiceTest | Always on sale AND claimed price never charged | INFLATED_ORIGINAL wins over PERPETUAL_SALE | INFLATED_ORIGINAL | Pass | 27 Nov 2026 |
| T30 | FR-23 | MySQL | After T24-T28, count saved verdicts of the test user | One verdict saved per observation | 5 rows, one of each verdict | Pass | 27 Nov 2026 |
| T31 | FR-34 | curl | GET /api/products/3/history?days=45 | 400 "days must be 30 or 90" | 400 "days must be 30 or 90" | Pass | 27 Nov 2026 |
| T32 | FR-07, FR-22 | Headless Chrome: real adapters + content.js + background.js against the server | Open the 5 demo store pages while logged in | Each page read correctly; badge drawn next to the price with the right verdict | genuine GENUINE, inflated INFLATED_ORIGINAL, hike PRICE_HIKE_BEFORE_SALE, perpetual PERPETUAL_SALE, new NOT_ENOUGH_DATA; badge right after the price box on all 5 (screenshots checked) | Pass | 27 Nov 2026 |
| T33 | FR-09 | Headless Chrome | Open the demo backpack page while logged out | Nothing sent to the server; page asks to log in | Background refused with "Please log in to Price Pulse."; badge "Log in to Price Pulse (toolbar icon) ..." | Pass | 27 Nov 2026 |
| T34 | FR-08 | Headless Chrome | Open the demo store product LIST page | No badge, no message | No badge, 0 messages sent | Pass | 27 Nov 2026 |
| T35 | FR-07, NFR-03 | Node, DarazAdapter | URL /products/20l-hmw-20skb-i1969609715-s14056026528.html?scm=...&pvid=... | SKU from the URL; address sent without the tracking part | SKU 1969609715-14056026528; url ends in .html (no "?") | Pass | 27 Nov 2026 |
| T36 | FR-10 (client) | Node, BaseAdapter.parsePrice | "Rs. 12,649", "₨ 5,999.50", "Rs. 0", "abc" | 12649, 5999.50, refused, refused | 12649, 5999.50, null, null | Pass | 27 Nov 2026 |
| T37 | FR-07 | Manual (Maryam), real Daraz page | Haier 20L Solo Microwave page on daraz.pk with the extension, logged in | Correct SKU and price reach the server; verdict NOT_ENOUGH_DATA for a new product | Database row: SKU 1969609715-14056026528, price 12649.00 (page showed Rs. 12,649), ACCEPTED, verdict NOT_ENOUGH_DATA. Badge on the page: confirm by screenshot | Pass (server side) | 26 Nov 2026 |
| T38 | FR-24, FR-33 | curl | Add demo earbuds to watchlist with target 3600 | 201; item armed with current price and verdict | 201, alertArmed true, currentPrice 3499.00, verdict GENUINE | Pass | 27 Nov 2026 |
| T39 | FR-25 | curl | Add the same product again | 409 "Already in your watchlist" | 409 "Already in your watchlist" | Pass | 27 Nov 2026 |
| T40 | FR-24 | curl | Target price 0 (add and change) | 400 "Target price must be greater than zero" | 400 "Target price must be greater than zero" (both) | Pass | 27 Nov 2026 |
| T41 | FR-26 | JUnit WatchlistServiceTest | 19 items then 20 items already on the list | 20th accepted, 21st refused | 20th accepted; 21st "Watchlist limit reached (20)" | Pass | 27 Nov 2026 |
| T42 | FR-28, FR-30 | curl + live stream | Watcher has the stream open; another shopper records Rs 3,499 (target 3,600) | One notification pushed live within 5 s, item disarmed | Event "notification" arrived after 608 ms: "... is now Rs 3,499, at or below your target of Rs 3,600."; alertArmed false | Pass | 27 Nov 2026 |
| T43 | FR-28 | curl | A third shopper records Rs 3,450 (still below target) | No second notification | Unread count stays 1 | Pass | 27 Nov 2026 |
| T44 | FR-29 | curl | Price Rs 4,300 (above target) recorded | Item armed again | alertArmed true | Pass | 27 Nov 2026 |
| T45 | FR-32 | curl | Mark the notification read; another shopper tries the same | Count 0; other shopper refused | Unread count 0; other shopper 404 "Notification not found" | Pass | 27 Nov 2026 |
| T47 | FR-22 | Headless Chrome: real popup + background.js + server | Open the popup after visiting the demo smart watch | Same verdict as the page badge, with claimed and real discount | "Inflated Discount" (red), "Claimed 40% \| Real 4%", reason shown | Pass | 27 Nov 2026 |
| T48 | FR-24, FR-25 | Headless Chrome popup | Watch with target 5800, then Watch again | Watching message; second time "Already in your watchlist" | As expected | Pass | 27 Nov 2026 |
| T49 | FR-28, FR-31 | Headless Chrome popup | Another shopper records Rs 5,750; reopen the popup | Alerts tab count 1, notification text, toolbar badge "1" | Count 1; "DEMO Smart Watch X2 is now Rs 5,750, at or below your target of Rs 5,800."; toolbar badge texts included "1" | Pass | 27 Nov 2026 |
| T50 | FR-32 | Headless Chrome popup | Press "Mark as read" | Count bubble hidden, button gone | First run: "0" bubble still visible (bug B4). After fix: bubble display none, no button (screenshot checked) | Pass after fix | 27 Nov 2026 |
| T51 | FR-27, FR-33 | Headless Chrome popup | Watchlist tab, then the remove (x) button | Row with store, now, target, verdict; then removed | "Demo Store ... \| Now Rs 5,750 \| Target Rs 5,800", tag "Inflated Discount"; list emptied | Pass | 27 Nov 2026 |
| T46 | FR-27 | curl | Another shopper deletes my watch item; then I delete it | Refused; then deleted | 404 "Watch item not found"; then 204 | Pass | 27 Nov 2026 |
