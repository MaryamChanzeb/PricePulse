package com.pricepulse.rules;

import com.pricepulse.TestRules;
import com.pricepulse.model.Verdict;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-17: rise of at least 15% in the 14 days before the sale, and the sale price not lower than before. */
class PreSaleHikeRuleTest {

    private final DiscountRule rule = new PreSaleHikeRule(TestRules.defaults());

    @Test
    void raisedThenReducedBackIsAHike() {
        History h = new History().at(20, "1000").at(10, "1200");
        Optional<Finding> finding = rule.evaluate(h.newestFirst(), h.now("1000", "1200", true));

        assertThat(finding).isPresent();
        assertThat(finding.get().verdict()).isEqualTo(Verdict.PRICE_HIKE_BEFORE_SALE);
        assertThat(finding.get().reason()).contains("Rs 1,000").contains("Rs 1,200");
    }

    @Test
    void exactlyFifteenPercentCounts() {
        History h = new History().at(20, "1000").at(10, "1150");
        assertThat(rule.evaluate(h.newestFirst(), h.now("1100", "1150", true))).isPresent();
    }

    @Test
    void justUnderFifteenPercentDoesNotCount() {
        History h = new History().at(20, "1000").at(10, "1149");
        assertThat(rule.evaluate(h.newestFirst(), h.now("1100", "1149", true))).isEmpty();
    }

    @Test
    void salePriceBelowThePriceBeforeTheRiseIsARealDiscount() {
        History h = new History().at(20, "1000").at(10, "1200");
        assertThat(rule.evaluate(h.newestFirst(), h.now("950", "1200", true))).isEmpty();
    }

    @Test
    void pageWithoutAnySaleIsNotJudged() {
        History h = new History().at(20, "1000").at(10, "1200");
        assertThat(rule.evaluate(h.newestFirst(), h.now("1000", null, false))).isEmpty();
    }

    @Test
    void riseOlderThanFourteenDaysBeforeTheSaleDoesNotCount() {
        History h = new History().at(40, "1000").at(30, "1300");
        assertThat(rule.evaluate(h.newestFirst(), h.now("1050", "1300", true))).isEmpty();
    }

    @Test
    void windowIsCountedFromWhenTheSalePriceFirstAppeared() {
        // Sale price 1100 first seen 3 days ago; the rise to 1200 happened 12 days ago, from 1000 (20 days ago).
        History h = new History().at(20, "1000").at(12, "1200").at(3, "1100", "1200", true);
        assertThat(rule.evaluate(h.newestFirst(), h.now("1100", "1200", true))).isPresent();
    }
}
