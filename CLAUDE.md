# Price Pulse (SEL-311 Software Construction, Bahria University)

## Project
Chrome extension (Manifest V3) + Spring Boot server + MySQL + web dashboard.
The extension reads the price on a supported store product page (first store: Daraz) and shows a
fake discount verdict. Shoppers can watch products with a target price and get live alerts.
Requirements are in docs/Price_Pulse_SRS (FR-01 to FR-42). Design is in docs/Price_Pulse_SDS.
Always follow those two documents. If you think they should change, tell me first.

## Team and rules about who writes code
- Two students: Maryam (extension + dashboard) and Zahra (server, database, rules). Both must be able to explain everything.
- COACH MODE is the default. Do NOT write complete feature code for me.
  For each task: (1) explain what and why in plain words, (2) show class and method signatures with
  TODO comments, (3) wait for me to write the bodies, (4) review my code for bugs, design problems and
  missing course requirements, (5) suggest test cases.
- You MAY directly create only: pom.xml, empty folder structure, manifest.json boilerplate, example
  config file with placeholders, and .gitignore.
- If I write "WRITE THIS ONE:" before a request, you may write that single piece, then explain it line by line.

## Stack
Java 17, Spring Boot, Spring Data JPA, MySQL 8, JUnit 5, Mockito, BCrypt, JWT.
Extension: plain JavaScript, no build tools. Dashboard: HTML, CSS, JavaScript, Bootstrap.

## Layout
server/src/main/java/com/pricepulse/{controller,service,repository,model,rules,exception,security,config}
extension/{manifest.json, background.js, content.js, adapters/BaseAdapter.js, adapters/DarazAdapter.js, popup.html, popup.js}
database/{schema.sql, seed.sql}   docs/   PROGRESS_LOG.md   README.md

## Code rules (course requirements)
- Layers: controller -> service -> repository -> database. Controllers never call repositories.
- Private fields, changes only through methods (encapsulation).
- Use List and Map with loops in the logic (collections and iteration).
- Inheritance: abstract User -> Shopper, Admin; abstract DiscountRule -> three rules; abstract AlertRule -> two rules.
  Services call the overridden methods through the parent type (polymorphism).
- Custom exceptions + one GlobalExceptionHandler. Never show a stack trace to the user.
- Thresholds (10, 15, 60, 14, 30, 80 percent) come from application.properties, not hard-coded.
- Keep ONE refactoring example: first version of POST /api/observations is allowed to keep validation and
  alert checks inside the controller; later we move them to services. Remind me to save before/after code.

## Security
- Never put passwords, keys or tokens in the repository. Read them from environment variables.
- Passwords stored as BCrypt hashes. Role checks on the server. Parameterised queries only.

## Git rules (very important)
- NEVER run git add, commit, push, merge, rebase or reset. We commit under our own accounts (plan in docs/COMMIT_PLAN.md).
- AI-written commits carry "Co-authored-by: Claude <noreply@anthropic.com>" in the message body.
- When a small step is finished, suggest a commit message that says what changed (for example
  "Add validation for non-positive prices in ObservationService"). Never "update", "fix" or "final".
- One branch per feature (feature/auth, feature/discount-rules, ...). Merge through a pull request that the
  other member reviews. Never rewrite history.
- Remind me to add a row to PROGRESS_LOG.md every week.

## Definition of done for each feature
Works end to end, has validation and clear error messages, has JUnit tests for the rules/services,
is mapped to FR numbers in the SRS, and both of us can explain it.
