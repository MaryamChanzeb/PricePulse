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
| T37 | FR-07 | Manual, real Daraz page | Open a real Daraz product page with the extension | Badge with a verdict (first visit: Not enough data yet) | NOT RUN YET - the Daraz selectors are unverified; check in Chrome | Open | |
