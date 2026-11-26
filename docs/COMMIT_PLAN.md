# Price Pulse - Commit plan

Claude Code writes code but never commits. Each member runs her own commands below, under her own
GitHub account, on the real day she does it. No backdating, no history rewriting.

Rules for every commit:
1. Check who you are first: `git config user.name; git config user.email`. Change it if it is the wrong person.
2. `git add` only the paths listed. Run `git status` before committing: `application-local.properties`,
   `.env` and `target/` must NEVER appear.
3. Code written with Claude carries `Co-authored-by: Claude <noreply@anthropic.com>` (the second `-m` below).
4. Open the pull request from your own GitHub login (use your own Chrome profile). The OTHER member reviews and approves it.

Pacing: each member needs at least 15 meaningful commits on at least 5 different days. Do this by building
one or two slices per working day, not by holding finished code back. Tests, review fixes and documents are real work too.

Identities (set before each slice; `--local` means only this repository):

```powershell
# Maryam
git config --local user.name "Maryam Chanzeb"
git config --local user.email "09-131242-044@student.bahria.edu.pk"
# Zahra
git config --local user.name "Zahra Latif"
git config --local user.email "<Zahra's GitHub email>"
```
The email must be one that is added and verified in that person's GitHub settings (Settings > Emails),
otherwise GitHub cannot link the commit to her account.

---

## Day 1 (26 Nov 2026)

### Step 0 - connect this folder to GitHub (Maryam, once)

The GitHub repository already has two commits (README.md, PROGRESS_LOG.md, gitignore.txt). This links
the local folder to that history without overwriting any local file.

```powershell
cd C:\Users\User\Documents\PricePulse
Remove-Item -Recurse -Force .github          # left behind by the VS Code Java upgrade tool
git init -b main
git remote add origin https://github.com/MaryamChanzeb/PricePulse.git
git fetch origin
git reset origin/main                         # points main at GitHub's commits; keeps all local files as they are
git branch --set-upstream-to=origin/main main
git status                                    # application-local.properties and target/ must NOT be listed
```
Also copy the four PDFs (proposal, SRS, SDS, wireframes) into `docs\` and delete `docs\.gitkeep` and `database\.gitkeep`.

### Slice S0 - project setup (runs: Maryam, reviews: Zahra)

```powershell
git switch -c feature/project-setup-maryam
git config --local user.name "Maryam Chanzeb"; git config --local user.email "09-131242-044@student.bahria.edu.pk"

git rm gitignore.txt
git add .gitignore
git commit -m "Replace gitignore.txt with a working .gitignore for Maven, IDE files and local secrets"

git add README.md PROGRESS_LOG.md CLAUDE.md docs/INSTRUCTOR_PERMISSION.md docs/COMMIT_PLAN.md docs/CONTRIBUTIONS.md docs/*.pdf
git commit -m "Add project rules, AI permission note, commit plan and design documents to docs" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/pom.xml server/src
git commit -m "Add Spring Boot server skeleton with layered packages and example configuration" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add extension/manifest.json extension/content.js extension/adapters
git commit -m "Add Manifest V3 extension skeleton with Daraz content script entry" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git push -u origin feature/project-setup-maryam
```
Pull request: "Project setup: server and extension skeleton". Reviewer: Zahra. Merge before Z1 and M1.

After the merge, both: `git switch main; git pull`.

### Slice Z1 - database scripts (runs: Zahra, reviews: Maryam)
FR-03, FR-13, FR-14, FR-23, FR-25; SDS section 5.

```powershell
git switch main; git pull
git switch -c feature/database-scripts-zahra
git config --local user.name "Zahra Latif"; git config --local user.email "<Zahra's GitHub email>"

git add database/schema.sql
git commit -m "Add MySQL schema with seven tables, foreign keys, unique keys and indexes" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add database/seed.sql
git commit -m "Add store seed data for Daraz, demo store and disabled example store" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add database/seed_demo_history.sql
git commit -m "Add labelled demo price history for all five verdict types" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git push -u origin feature/database-scripts-zahra
```
Pull request: "Database schema and seed scripts (Z1)". Reviewer: Maryam.

### Slice M1 - popup shell and background worker (runs: Maryam, reviews: Zahra)
FR-09 (ask to log in first), FR-42 (offline message); SDS 3.4.

```powershell
git switch main; git pull
git switch -c feature/popup-shell-maryam
git config --local user.name "Maryam Chanzeb"; git config --local user.email "09-131242-044@student.bahria.edu.pk"

git add extension/background.js
git commit -m "Add background worker with API client, offline detection and session storage" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add extension/popup.html extension/popup.css extension/popup.js
git commit -m "Add popup shell with offline, login and account views in teal and amber theme" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git push -u origin feature/popup-shell-maryam
```
Pull request: "Popup shell and background worker (M1)". Reviewer: Zahra.

Count after Day 1: Maryam 6 commits (S0 + M1), Zahra 3 commits (Z1).

---

## Next days: commit each slice on the day the committer has read it, run it and can explain it

### Slice Z2 - entities and repositories (runs: Zahra, reviews: Maryam)
FR-03, FR-13, FR-14, FR-23 to FR-29, FR-32; SDS 3.1, 3.3, 5.

```powershell
git switch main; git pull
git switch -c feature/entities-zahra
git config --local user.name "Zahra Latif"; git config --local user.email "<Zahra's GitHub email>"

git add server/src/main/resources/application.properties server/src/main/resources/application-example.properties
git commit -m "Rename rule thresholds to pricepulse.rules keys and store all times in UTC" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git rm server/src/main/java/com/pricepulse/model/.gitkeep
git add server/src/main/java/com/pricepulse/model/Role.java server/src/main/java/com/pricepulse/model/ModelChecks.java server/src/main/java/com/pricepulse/model/User.java server/src/main/java/com/pricepulse/model/Shopper.java server/src/main/java/com/pricepulse/model/Admin.java server/src/test/java/com/pricepulse/model/UserTest.java
git commit -m "Add User, Shopper and Admin entities with single-table inheritance" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/model/Store.java server/src/main/java/com/pricepulse/model/Product.java server/src/main/java/com/pricepulse/model/ObservationStatus.java server/src/main/java/com/pricepulse/model/PriceObservation.java server/src/test/java/com/pricepulse/model/PriceObservationTest.java
git commit -m "Add Store, Product and PriceObservation entities with status methods" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/model/Verdict.java server/src/main/java/com/pricepulse/model/DiscountVerdict.java server/src/main/java/com/pricepulse/model/WatchItem.java server/src/main/java/com/pricepulse/model/Notification.java server/src/test/java/com/pricepulse/model/WatchItemTest.java server/src/test/java/com/pricepulse/model/StoreAndNotificationTest.java
git commit -m "Add verdict, watch item and notification entities with arm and disarm" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git rm server/src/main/java/com/pricepulse/repository/.gitkeep
git add server/src/main/java/com/pricepulse/repository
git commit -m "Add Spring Data repositories for the seven tables" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git push -u origin feature/entities-zahra
```
Pull request: "Entities and repositories (Z2)". Reviewer: Maryam.

### Slice Z3 - login, roles and error handling (runs: Zahra, reviews: Maryam)
FR-01 to FR-06, FR-40; SDS 3.1, 6, 8, 10.

```powershell
git switch main; git pull
git switch -c feature/auth-zahra
git config --local user.name "Zahra Latif"; git config --local user.email "<Zahra's GitHub email>"

git rm server/src/main/java/com/pricepulse/exception/.gitkeep
git add server/src/main/java/com/pricepulse/exception
git commit -m "Add ApiException hierarchy and global JSON error handler" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git rm server/src/main/java/com/pricepulse/security/.gitkeep
git add server/src/main/java/com/pricepulse/security server/src/test/java/com/pricepulse/security server/src/test/java/com/pricepulse/TestUsers.java
git commit -m "Add JWT service and token filter with 24 hour expiry" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git rm server/src/main/java/com/pricepulse/service/.gitkeep
git add server/src/main/java/com/pricepulse/dto server/src/main/java/com/pricepulse/service/AuthService.java server/src/test/java/com/pricepulse/service/AuthServiceTest.java
git commit -m "Add AuthService for shopper sign-up, login and BCrypt hashing" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git rm server/src/main/java/com/pricepulse/config/.gitkeep server/src/main/java/com/pricepulse/controller/.gitkeep
git add server/src/main/java/com/pricepulse/config server/src/main/java/com/pricepulse/controller/AuthController.java server/src/main/java/com/pricepulse/PricePulseApplication.java server/src/test/java/com/pricepulse/controller/SecurityRulesTest.java
git commit -m "Add role rules, auth endpoints and admin account creation at startup" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add docs/CHANGES.md docs/test-table.md docs/bug-log.md
git commit -m "Add design changes list, first real test results and bug log"

git push -u origin feature/auth-zahra
```
Pull request: "Login, roles and error handling (Z3)". Reviewer: Maryam.

### Slice Z4 - observation endpoint, first version (runs: Zahra, reviews: Maryam)
FR-07, FR-10 to FR-14. This is the "before" code of the refactoring example: do NOT clean it up yet.

```powershell
git switch main; git pull
git switch -c feature/observations-v1-zahra
git config --local user.name "Zahra Latif"; git config --local user.email "<Zahra's GitHub email>"

git rm server/src/main/java/com/pricepulse/rules/.gitkeep
git add server/src/main/java/com/pricepulse/rules/RuleConfig.java server/src/main/java/com/pricepulse/PricePulseApplication.java server/src/test/java/com/pricepulse/TestRules.java
git commit -m "Add RuleConfig that reads all rule thresholds from application.properties" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/exception/PriceValidationException.java server/src/main/java/com/pricepulse/exception/UnsupportedStoreException.java server/src/main/java/com/pricepulse/exception/NotLoggedInException.java server/src/main/java/com/pricepulse/dto/ObservationRequest.java server/src/main/java/com/pricepulse/dto/ObservationResponse.java
git commit -m "Add observation request and response records and price, store exceptions" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/controller/ObservationController.java server/src/test/java/com/pricepulse/controller/ObservationControllerTest.java
git commit -m "Add first version of the observation endpoint with validation in the controller" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add docs/refactoring-before.md docs/test-table.md docs/bug-log.md
git commit -m "Record refactoring before-code, observation test results and rounding bug B2"

git push -u origin feature/observations-v1-zahra
```
Pull request: "Observation endpoint, first version (Z4)". Reviewer: Maryam.

### Slice M2 - popup log in and sign up (runs: Maryam, reviews: Zahra)
FR-01, FR-04, FR-05, FR-09. Needs Z3 merged to test against the real server.

```powershell
git switch main; git pull
git switch -c feature/popup-login-maryam
git config --local user.name "Maryam Chanzeb"; git config --local user.email "09-131242-044@student.bahria.edu.pk"

git add extension/background.js
git commit -m "Add LOGIN and SIGNUP messages to the background worker for shopper accounts" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add extension/popup.html extension/popup.css
git commit -m "Add log in and sign up tabs with show password to the popup" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add extension/popup.js
git commit -m "Validate popup forms and show server messages for log in and sign up" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add docs/CONTRIBUTIONS.md
git commit -m "List slices Z2 to M2 in the contributions table"

git push -u origin feature/popup-login-maryam
```
Pull request: "Popup log in and sign up (M2)". Reviewer: Zahra.

### Slice Z5 - discount rules and verdicts (runs: Zahra, reviews: Maryam)
FR-15 to FR-23, FR-34; SDS 3.1 (Figure 3), 4.

```powershell
git switch main; git pull
git switch -c feature/discount-rules-zahra
git config --local user.name "Zahra Latif"; git config --local user.email "<Zahra's GitHub email>"

git add server/src/main/java/com/pricepulse/rules/PricePoint.java server/src/main/java/com/pricepulse/rules/Finding.java server/src/main/java/com/pricepulse/rules/Prices.java server/src/main/java/com/pricepulse/rules/DiscountRule.java server/src/test/java/com/pricepulse/rules/PricesTest.java
git commit -m "Add abstract DiscountRule with price point, finding and price helpers" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/rules/InflatedOriginalPriceRule.java server/src/main/java/com/pricepulse/rules/PreSaleHikeRule.java server/src/main/java/com/pricepulse/rules/PerpetualSaleRule.java
git commit -m "Add inflated original, pre-sale hike and perpetual sale rules" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/test/java/com/pricepulse/rules/History.java server/src/test/java/com/pricepulse/rules/InflatedOriginalPriceRuleTest.java server/src/test/java/com/pricepulse/rules/PreSaleHikeRuleTest.java server/src/test/java/com/pricepulse/rules/PerpetualSaleRuleTest.java
git commit -m "Add JUnit tests with prepared price histories for each rule threshold" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/rules/VerdictResult.java server/src/main/java/com/pricepulse/service/DiscountAnalysisService.java server/src/test/java/com/pricepulse/rules/DiscountAnalysisServiceTest.java
git commit -m "Add DiscountAnalysisService with enough-data check, rule priority and real discount" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/dto/VerdictResponse.java server/src/main/java/com/pricepulse/dto/ObservationResponse.java server/src/main/java/com/pricepulse/controller/ObservationController.java server/src/test/java/com/pricepulse/controller/ObservationControllerTest.java
git commit -m "Return the saved verdict from the observation endpoint" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add server/src/main/java/com/pricepulse/exception/ProductNotFoundException.java server/src/main/java/com/pricepulse/dto/ProductHistoryResponse.java server/src/main/java/com/pricepulse/service/ProductService.java server/src/main/java/com/pricepulse/controller/ProductController.java server/src/test/java/com/pricepulse/service/ProductServiceTest.java
git commit -m "Add product verdict and 30 or 90 day history endpoints" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add docs/refactoring-before.md docs/test-table.md docs/bug-log.md docs/CONTRIBUTIONS.md docs/COMMIT_PLAN.md
git commit -m "Record verdict test results, test helper bug B3 and updated before-code"

git push -u origin feature/discount-rules-zahra
```
Pull request: "Discount rules and verdicts (Z5)". Reviewer: Maryam - she should read the three rule classes and check them against SDS 4.2.

### Slice M3 - store adapters and demo store (runs: Maryam, reviews: Zahra)
FR-07, FR-08, NFR-03; SDS 3.4. Before committing DarazAdapter: open a real Daraz page, check the selectors in
DevTools (F12) and save the page into extension/test-pages/ (see the note at the top of DarazAdapter.js).

```powershell
git switch main; git pull
git switch -c feature/store-adapters-maryam
git config --local user.name "Maryam Chanzeb"; git config --local user.email "09-131242-044@student.bahria.edu.pk"

git add extension/adapters/BaseAdapter.js
git commit -m "Add BaseAdapter with readObservation template, price parsing and clean URL" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add extension/adapters/DarazAdapter.js
git commit -m "Add DarazAdapter reading structured data first and visible price elements second" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git rm server/src/main/resources/static/.gitkeep
git add server/src/main/resources/static/demo-store server/src/main/java/com/pricepulse/config/WebConfig.java extension/adapters/DemoStoreAdapter.js
git commit -m "Add labelled demo store pages and DemoStoreAdapter for the class demonstration" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git push -u origin feature/store-adapters-maryam
```
Pull request: "Store adapters and demo store (M3)". Reviewer: Zahra.

### Slice M4 - content script and verdict badge (runs: Maryam, reviews: Zahra)
FR-07, FR-08, FR-09, FR-22, FR-42.

```powershell
git switch main; git pull
git switch -c feature/content-badge-maryam
git config --local user.name "Maryam Chanzeb"; git config --local user.email "09-131242-044@student.bahria.edu.pk"

git add extension/background.js
git commit -m "Add OBSERVE message that sends a read page only for logged-in shoppers" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add extension/content.js extension/manifest.json
git commit -m "Add content script that reads the product and draws the verdict badge in a shadow DOM" -m "Co-authored-by: Claude <noreply@anthropic.com>"

git add docs/test-table.md docs/CONTRIBUTIONS.md docs/COMMIT_PLAN.md
git commit -m "Record browser test results for the demo store badges"

git push -u origin feature/content-badge-maryam
```
Pull request: "Content script and verdict badge (M4)". Reviewer: Zahra.
