# Refactoring example - BEFORE

Course requirement: one refactoring with before and after code. This is the **first version** of
`POST /api/observations` (slice Z4, file `server/src/main/java/com/pricepulse/controller/ObservationController.java`).
It works correctly (FR-10 to FR-14, 15 JUnit tests pass) but its structure is deliberately poor.

> Slices Z5 and Z6 add the verdict and the alert check to this same controller method before it is refactored.
> This file is updated with the final "before" code just before the refactoring in slice Z8.

## Problems (code smells)

| Smell | Where | Why it is a problem |
|-------|-------|---------------------|
| Layer violation | The controller holds `UserRepository`, `StoreRepository`, `ProductRepository`, `PriceObservationRepository` | SDS 2.1 and CLAUDE.md: controller -> service -> repository. The web layer now knows the database. |
| Long method | `submit()` does 8 steps in about 90 lines | One method with many reasons to change; hard to read and to explain. |
| Business rules in the controller | Price check, duplicate window, outlier rule, product creation | Rules cannot be reused (for example by an admin import) or tested without the web layer. |
| Transaction in the web layer | `@Transactional` on the controller method | Transactions belong to the service that owns the business operation. |
| Private helpers that belong elsewhere | `parsePrice`, `domainOf` | Validation and URL handling are not web concerns. |

## The code (Z4 version, plus the verdict step added in Z5)

```java
@PostMapping
@Transactional
public ObservationResponse submit(@AuthenticationPrincipal AuthenticatedUser caller,
                                  @RequestBody ObservationRequest request) {
    // 1. Who is sending it? (the id comes from the token, never from the request body)
    User user = userRepository.findById(caller.id()).orElseThrow(NotLoggedInException::new);

    // 2. Required product details (FR-07)
    if (request == null) {
        throw new ValidationException("Observation data is required");
    }
    if (isBlank(request.url())) {
        throw new ValidationException("Page address is required");
    }
    if (isBlank(request.sku())) {
        throw new ValidationException("Product SKU is required");
    }
    if (isBlank(request.title())) {
        throw new ValidationException("Product title is required");
    }
    String sku = request.sku().trim();
    if (sku.length() > 100) {
        throw new ValidationException("Product SKU is too long");
    }

    // 3. Prices (FR-10): positive number, not above the maximum
    BigDecimal price = parsePrice(request.price());
    BigDecimal claimedOriginal = null;
    if (!isBlank(request.claimedOriginalPrice())) {
        claimedOriginal = parsePrice(request.claimedOriginalPrice());
        if (claimedOriginal.compareTo(price) <= 0) {
            claimedOriginal = null;
        }
    }
    boolean badgeShown = Boolean.TRUE.equals(request.discountBadgeShown());

    // 4. Supported store (FR-11)
    String domain = domainOf(request.url());
    Optional<Store> foundStore = storeRepository.findByDomain(domain);
    if (foundStore.isEmpty() || !foundStore.get().isSupported()) {
        throw new UnsupportedStoreException();
    }
    Store store = foundStore.get();

    // 5. Product: create it the first time this SKU is seen (FR-14), otherwise refresh its details
    String title = truncate(request.title().trim(), 500);
    String url = truncate(request.url().trim(), 1000);
    String imageUrl = isBlank(request.imageUrl()) ? null : truncate(request.imageUrl().trim(), 1000);
    Product product;
    Optional<Product> existingProduct = productRepository.findByStoreAndStoreSku(store, sku);
    if (existingProduct.isPresent()) {
        product = existingProduct.get();
        product.refreshDetails(title, url, imageUrl);
    } else {
        product = productRepository.save(new Product(store, sku, title, url, imageUrl));
    }

    // 6. Same user, same product within the duplicate window: ignore it (FR-12)
    Instant now = Instant.now();
    Instant windowStart = now.minus(Duration.ofHours(rules.duplicateWindowHours()));
    Optional<PriceObservation> recent = observationRepository
            .findFirstByProductAndUserAndObservedAtAfterOrderByObservedAtDesc(product, user, windowStart);
    if (recent.isPresent()) {
        return new ObservationResponse(recent.get().getId(), product.getId(), product.getTitle(),
                recent.get().getStatus(), true,
                "You already recorded this product in the last " + rules.duplicateWindowHours() + " hours",
                latestVerdict(product));
    }

    // 7. Outlier: more than flag-change-percent away from the last accepted price -> FLAGGED (FR-13)
    PriceObservation observation = new PriceObservation(product, user, price, claimedOriginal, badgeShown, now);
    Optional<PriceObservation> lastAccepted = observationRepository
            .findFirstByProductAndStatusOrderByObservedAtDesc(product, ObservationStatus.ACCEPTED);
    if (lastAccepted.isPresent()) {
        BigDecimal lastPrice = lastAccepted.get().getPrice();
        BigDecimal changeTimes100 = price.subtract(lastPrice).abs().multiply(BigDecimal.valueOf(100));
        BigDecimal limitTimesLast = lastPrice.multiply(BigDecimal.valueOf(rules.flagChangePercent()));
        if (changeTimes100.compareTo(limitTimesLast) > 0) {
            observation.markFlagged();
        }
    }

    // 8. Save
    PriceObservation saved = observationRepository.save(observation);

    // 9. Verdict: analyse an accepted price (FR-15 to FR-23); a flagged price is left out of analysis (FR-13)
    VerdictResponse verdict;
    String message;
    if (saved.isAccepted()) {
        verdict = VerdictResponse.from(discountAnalysisService.analyse(saved));
        message = "Price recorded";
    } else {
        verdict = latestVerdict(product);
        message = "This price changed by more than " + rules.flagChangePercent() + "% and is held for review";
    }
    return new ObservationResponse(saved.getId(), product.getId(), product.getTitle(),
            saved.getStatus(), false, message, verdict);
}

private VerdictResponse latestVerdict(Product product) {
    return verdictRepository.findFirstByProductOrderByCreatedAtDesc(product)   // a 5th repository in the controller
            .map(VerdictResponse::from)
            .orElse(VerdictResponse.notAnalysedYet(product.getId()));
}
```

In slice Z5 the controller gained a 5th repository (`DiscountVerdictRepository`) and a service, and its
constructor now takes 7 dependencies - another sign that it does too much.
