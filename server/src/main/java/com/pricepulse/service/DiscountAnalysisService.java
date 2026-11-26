package com.pricepulse.service;

import com.pricepulse.model.DiscountVerdict;
import com.pricepulse.model.ObservationStatus;
import com.pricepulse.model.PriceObservation;
import com.pricepulse.model.Verdict;
import com.pricepulse.repository.DiscountVerdictRepository;
import com.pricepulse.repository.PriceObservationRepository;
import com.pricepulse.rules.DiscountRule;
import com.pricepulse.rules.Finding;
import com.pricepulse.rules.PricePoint;
import com.pricepulse.rules.Prices;
import com.pricepulse.rules.RuleConfig;
import com.pricepulse.rules.VerdictResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns the price history of a product into a verdict (SDS 4, Figure 6; FR-15 to FR-21, FR-23).
 *
 *  1. Enough data? (FR-15) If not: NOT_ENOUGH_DATA.
 *  2. Real discount against the 30-day median (FR-21).
 *  3. Run EVERY rule in the list (polymorphism: each element is just a DiscountRule) and collect findings.
 *  4. Several findings? Keep the one with the highest priority (FR-20). None? GENUINE (FR-19).
 */
@Service
public class DiscountAnalysisService {

    private final List<DiscountRule> rules;
    private final RuleConfig config;
    private final PriceObservationRepository observationRepository;
    private final DiscountVerdictRepository verdictRepository;

    /** Spring passes in every @Component that extends DiscountRule. */
    public DiscountAnalysisService(List<DiscountRule> rules, RuleConfig config,
                                   PriceObservationRepository observationRepository,
                                   DiscountVerdictRepository verdictRepository) {
        this.rules = List.copyOf(rules);
        this.config = config;
        this.observationRepository = observationRepository;
        this.verdictRepository = verdictRepository;
    }

    /** Analyses a just-saved ACCEPTED observation and saves the verdict with it (FR-23). */
    @Transactional
    public DiscountVerdict analyse(PriceObservation observation) {
        List<PriceObservation> accepted = observationRepository
                .findByProductAndStatusOrderByObservedAtDesc(observation.getProduct(), ObservationStatus.ACCEPTED);
        List<PricePoint> history = new ArrayList<>();
        for (PriceObservation each : accepted) {
            history.add(PricePoint.from(each));
        }
        VerdictResult result = evaluate(history, PricePoint.from(observation));
        return verdictRepository.save(new DiscountVerdict(observation.getProduct(), observation, result.verdict(),
                result.realDiscountPct(), result.claimedDiscountPct(), result.reason()));
    }

    /** The pure decision, with no database: easy to test with prepared histories. History is newest first. */
    public VerdictResult evaluate(List<PricePoint> history, PricePoint snapshot) {
        BigDecimal claimedPct = snapshot.claimedOriginalPrice() == null
                ? null
                : Prices.percentBelow(snapshot.claimedOriginalPrice(), snapshot.price());

        // 1. FR-15: at least 3 accepted observations whose first and last are at least 7 days apart
        if (!hasEnoughData(history)) {
            String reason = "Not enough price history yet: " + history.size() + " observation(s) over "
                    + spanInDays(history) + " day(s). Price Pulse needs at least " + config.minObservations()
                    + " over " + config.minHistoryDays() + " days.";
            return new VerdictResult(Verdict.NOT_ENOUGH_DATA, reason, null, claimedPct);
        }

        // 2. Real discount against the median of the last 30 days
        BigDecimal median = medianOfLastDays(history, snapshot.observedAt());
        BigDecimal realPct = Prices.percentBelow(median, snapshot.price());

        // 3. Every rule, through the parent type
        List<Finding> findings = new ArrayList<>();
        for (DiscountRule rule : rules) {
            Optional<Finding> finding = rule.evaluate(history, snapshot);
            finding.ifPresent(findings::add);
        }

        // 4. Highest priority wins (INFLATED_ORIGINAL > PRICE_HIKE_BEFORE_SALE > PERPETUAL_SALE)
        Finding worst = null;
        for (Finding finding : findings) {
            if (worst == null || finding.verdict().getPriority() > worst.verdict().getPriority()) {
                worst = finding;
            }
        }
        if (worst != null) {
            return new VerdictResult(worst.verdict(), worst.reason(), realPct, claimedPct);
        }
        return new VerdictResult(Verdict.GENUINE, genuineReason(realPct, median), realPct, claimedPct);
    }

    private boolean hasEnoughData(List<PricePoint> history) {
        if (history.size() < config.minObservations()) {
            return false;
        }
        return spanInDays(history) >= config.minHistoryDays();
    }

    /** Whole days between the oldest and the newest observation. */
    private long spanInDays(List<PricePoint> history) {
        if (history.isEmpty()) {
            return 0;
        }
        Instant oldest = history.get(0).observedAt();
        Instant newest = history.get(0).observedAt();
        for (PricePoint point : history) {
            if (point.observedAt().isBefore(oldest)) {
                oldest = point.observedAt();
            }
            if (point.observedAt().isAfter(newest)) {
                newest = point.observedAt();
            }
        }
        return Duration.between(oldest, newest).toDays();
    }

    private BigDecimal medianOfLastDays(List<PricePoint> history, Instant until) {
        Instant from = until.minus(Duration.ofDays(config.medianDays()));
        List<BigDecimal> prices = new ArrayList<>();
        for (PricePoint point : history) {
            if (!point.observedAt().isBefore(from) && !point.observedAt().isAfter(until)) {
                prices.add(point.price());
            }
        }
        return Prices.median(prices);
    }

    private String genuineReason(BigDecimal realPct, BigDecimal median) {
        if (realPct.signum() > 0) {
            return "The price history supports this discount: the price is " + Prices.percentText(realPct)
                    + " below the " + config.medianDays() + "-day median of " + Prices.rs(median) + ".";
        }
        return "No fake discount found, but the price is not below its " + config.medianDays()
                + "-day median of " + Prices.rs(median) + ".";
    }
}
