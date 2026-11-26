package com.pricepulse.controller;

import com.pricepulse.dto.ObservationRequest;
import com.pricepulse.dto.ObservationResponse;
import com.pricepulse.dto.VerdictResponse;
import com.pricepulse.exception.NotLoggedInException;
import com.pricepulse.exception.PriceValidationException;
import com.pricepulse.exception.UnsupportedStoreException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.model.ObservationStatus;
import com.pricepulse.model.PriceObservation;
import com.pricepulse.model.Product;
import com.pricepulse.model.Store;
import com.pricepulse.model.User;
import com.pricepulse.repository.DiscountVerdictRepository;
import com.pricepulse.repository.PriceObservationRepository;
import com.pricepulse.repository.ProductRepository;
import com.pricepulse.repository.StoreRepository;
import com.pricepulse.repository.UserRepository;
import com.pricepulse.rules.RuleConfig;
import com.pricepulse.security.AuthenticatedUser;
import com.pricepulse.service.DiscountAnalysisService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * POST /api/observations - FIRST VERSION, kept on purpose for the course refactoring example
 * (CLAUDE.md, SDS 7 "Layer boundaries", docs/refactoring-before.md).
 *
 * What is wrong with it (and will be fixed in slice Z8):
 *  - The controller calls four repositories directly. The rule is controller -> service -> repository.
 *  - All validation, the duplicate rule, the outlier rule and product creation live in this one
 *    long method, so they cannot be tested or reused on their own.
 *  - The transaction is started in the web layer.
 * The BEHAVIOUR is correct and is kept the same after the refactoring.
 */
@RestController
@RequestMapping("/api/observations")
public class ObservationController {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final PriceObservationRepository observationRepository;
    private final DiscountVerdictRepository verdictRepository;
    private final DiscountAnalysisService discountAnalysisService;
    private final RuleConfig rules;

    public ObservationController(UserRepository userRepository, StoreRepository storeRepository,
                                 ProductRepository productRepository, PriceObservationRepository observationRepository,
                                 DiscountVerdictRepository verdictRepository,
                                 DiscountAnalysisService discountAnalysisService, RuleConfig rules) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.observationRepository = observationRepository;
        this.verdictRepository = verdictRepository;
        this.discountAnalysisService = discountAnalysisService;
        this.rules = rules;
    }

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
                claimedOriginal = null; // a "was" price that is not higher than the price is not a discount claim
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
            // change% > limit  <=>  |new - last| * 100 > last * limit   (exact: no division, no rounding)
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
        return verdictRepository.findFirstByProductOrderByCreatedAtDesc(product)
                .map(VerdictResponse::from)
                .orElse(VerdictResponse.notAnalysedYet(product.getId()));
    }

    private BigDecimal parsePrice(String text) {
        if (isBlank(text)) {
            throw new PriceValidationException();
        }
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim());
        } catch (NumberFormatException notANumber) {
            throw new PriceValidationException();
        }
        if (value.signum() <= 0 || value.compareTo(rules.maxPrice()) > 0) {
            throw new PriceValidationException();
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** "https://www.daraz.pk/products/x.html" -> "daraz.pk"; "http://localhost:8080/demo-store/a.html" -> "localhost:8080" */
    private String domainOf(String pageUrl) {
        URI uri;
        try {
            uri = URI.create(pageUrl.trim());
        } catch (IllegalArgumentException badAddress) {
            throw new ValidationException("Invalid page address");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (host == null || scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ValidationException("Invalid page address");
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return uri.getPort() == -1 ? host : host + ":" + uri.getPort();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
