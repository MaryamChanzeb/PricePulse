package com.pricepulse.service;

import com.pricepulse.dto.WatchItemResponse;
import com.pricepulse.exception.DuplicateWatchException;
import com.pricepulse.exception.NotLoggedInException;
import com.pricepulse.exception.ProductNotFoundException;
import com.pricepulse.exception.ValidationException;
import com.pricepulse.exception.WatchLimitExceededException;
import com.pricepulse.model.ObservationStatus;
import com.pricepulse.model.PriceObservation;
import com.pricepulse.model.Product;
import com.pricepulse.model.User;
import com.pricepulse.model.Verdict;
import com.pricepulse.model.WatchItem;
import com.pricepulse.repository.DiscountVerdictRepository;
import com.pricepulse.repository.PriceObservationRepository;
import com.pricepulse.repository.ProductRepository;
import com.pricepulse.repository.UserRepository;
import com.pricepulse.repository.WatchItemRepository;
import com.pricepulse.rules.AlertRule;
import com.pricepulse.rules.RuleConfig;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Watchlist rules and price alerts (FR-24 to FR-29, SDS 4.3). */
@Service
public class WatchlistService {

    private final WatchItemRepository watchItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PriceObservationRepository observationRepository;
    private final DiscountVerdictRepository verdictRepository;
    private final List<AlertRule> alertRules;
    private final NotificationService notificationService;
    private final RuleConfig config;

    /** Spring passes every @Component that extends AlertRule, sorted by @Order. */
    public WatchlistService(WatchItemRepository watchItemRepository, ProductRepository productRepository,
                            UserRepository userRepository, PriceObservationRepository observationRepository,
                            DiscountVerdictRepository verdictRepository, List<AlertRule> alertRules,
                            NotificationService notificationService, RuleConfig config) {
        this.watchItemRepository = watchItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.observationRepository = observationRepository;
        this.verdictRepository = verdictRepository;
        this.alertRules = List.copyOf(alertRules);
        this.notificationService = notificationService;
        this.config = config;
    }

    /** FR-24 to FR-26: add a product with a target price. */
    @Transactional
    public WatchItemResponse add(Long userId, Long productId, BigDecimal targetPrice) {
        requireValidTarget(targetPrice);
        User user = findUser(userId);
        Product product = productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
        if (watchItemRepository.existsByUserAndProduct(user, product)) {
            throw new DuplicateWatchException();                              // FR-25
        }
        if (watchItemRepository.countByUser(user) >= config.watchlistLimit()) {
            throw new WatchLimitExceededException(config.watchlistLimit());   // FR-26
        }
        try {
            return toResponse(watchItemRepository.saveAndFlush(new WatchItem(user, product, targetPrice)));
        } catch (DataIntegrityViolationException sameTimeDuplicate) {
            throw new DuplicateWatchException(); // two clicks at once: the UNIQUE (user_id, product_id) key caught it
        }
    }

    /** FR-33: the shopper's watchlist with current price and latest verdict. */
    @Transactional(readOnly = true)
    public List<WatchItemResponse> list(Long userId) {
        List<WatchItemResponse> result = new ArrayList<>();
        for (WatchItem item : watchItemRepository.findByUserOrderByCreatedAtAsc(findUser(userId))) {
            result.add(toResponse(item));
        }
        return result;
    }

    /** FR-27: change the target. */
    @Transactional
    public WatchItemResponse changeTarget(Long userId, Long itemId, BigDecimal targetPrice) {
        requireValidTarget(targetPrice);
        WatchItem item = findOwnItem(userId, itemId);
        item.changeTarget(targetPrice);
        return toResponse(item);
    }

    /** FR-27: remove an item. */
    @Transactional
    public void remove(Long userId, Long itemId) {
        watchItemRepository.delete(findOwnItem(userId, itemId));
    }

    /**
     * SDS 4.3: called after every ACCEPTED observation. Loops over everyone watching the product.
     *  - armed item: ask each AlertRule (through the parent type); the first that fires sends ONE
     *    notification and the item is disarmed, so the same drop is never alerted twice (FR-28);
     *  - disarmed item: when the price is back above the target, arm it again (FR-29).
     * @return the number of notifications sent
     */
    @Transactional
    public int checkAlerts(Product product, BigDecimal previousPrice, BigDecimal newPrice) {
        int sent = 0;
        for (WatchItem item : watchItemRepository.findByProduct(product)) {
            BigDecimal target = item.getTargetPrice();
            if (item.isAlertArmed()) {
                for (AlertRule rule : alertRules) {
                    if (rule.isTriggered(previousPrice, newPrice, target)) {
                        notificationService.create(item.getUser(), product,
                                rule.message(product.getTitle(), previousPrice, newPrice, target));
                        item.disarm();
                        sent++;
                        break; // one notification per drop
                    }
                }
            } else if (newPrice.compareTo(target) > 0) {
                item.arm();
            }
        }
        return sent;
    }

    private WatchItemResponse toResponse(WatchItem item) {
        Product product = item.getProduct();
        BigDecimal currentPrice = observationRepository
                .findFirstByProductAndStatusOrderByObservedAtDesc(product, ObservationStatus.ACCEPTED)
                .map(PriceObservation::getPrice)
                .orElse(null);
        Verdict verdict = verdictRepository.findFirstByProductOrderByCreatedAtDesc(product)
                .map(saved -> saved.getVerdict())
                .orElse(Verdict.NOT_ENOUGH_DATA);
        return new WatchItemResponse(item.getId(), product.getId(), product.getTitle(), product.getStore().getName(),
                product.getUrl(), product.getImageUrl(), currentPrice, item.getTargetPrice(), item.isAlertArmed(),
                verdict, item.getCreatedAt());
    }

    private void requireValidTarget(BigDecimal targetPrice) {
        if (targetPrice == null || targetPrice.signum() <= 0) {
            throw new ValidationException("Target price must be greater than zero");
        }
        if (targetPrice.compareTo(config.maxPrice()) > 0) {
            throw new ValidationException("Target price is too high");
        }
    }

    private WatchItem findOwnItem(Long userId, Long itemId) {
        return watchItemRepository.findByIdAndUser(itemId, findUser(userId))
                .orElseThrow(() -> new ProductNotFoundException("Watch item not found"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(NotLoggedInException::new);
    }
}
