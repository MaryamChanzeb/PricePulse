# Changes since Assignment 3 (SRS and SDS v1.0)

One line per change with its reason. The SRS and SDS are otherwise followed as submitted.

| # | Document and section | Change | Reason |
|---|----------------------|--------|--------|
| 1 | SDS 8 | `AccessDeniedException` is named `RoleAccessDeniedException` | Spring Security already has a class called `AccessDeniedException`; two classes with one name cause wrong imports. |
| 2 | SDS 8 | All custom exceptions extend one abstract `ApiException` that knows its HTTP status | `GlobalExceptionHandler` needs one method for all of them instead of one per class; another use of polymorphism. |
| 3 | SRS 2.2 | The Admin account is created by the server at startup from `ADMIN_EMAIL` and `ADMIN_PASSWORD`, not by `seed.sql` | A seed script would have to contain a password hash, i.e. a secret in the repository (NFR-02). |
| 4 | SDS 2.2 | Extra package `com.pricepulse.dto` for request and response records | Keeps JSON shapes separate from the entities, so entities never leak password hashes to the client. |
| 5 | SDS 3.1, Figure 2 | `Shopper.watchLimit` is not a field; the limit 20 is `pricepulse.rules.watchlist-limit` | Thresholds belong in configuration (SDS 7, NFR-07); the same for every shopper. |
| 6 | SDS 5.1 | A third store row "Demo Store" (`localhost:8080`) and `seed_demo_history.sql` with labelled DEMO DATA | SRS 6 "Early data": every verdict type must be demonstrable in class without waiting for real history. |
| 7 | SDS 6 | Shopper endpoints (`/api/**` except auth and admin) require the SHOPPER role; admin endpoints require ADMIN | Follows the "Role" column of SDS 6 exactly; noted because an Admin therefore cannot use the shopper extension. |
