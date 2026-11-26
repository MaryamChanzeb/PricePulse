package com.pricepulse.rules;

import com.pricepulse.TestRules;
import com.pricepulse.model.Verdict;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-18: badge in at least 80% of at least 5 observations over the last 30 days or more. */
class PerpetualSaleRuleTest {

    private final DiscountRule rule = new PerpetualSaleRule(TestRules.defaults());

    @Test
    void eightOfTenWithBadgeIsAPerpetualSale() {
        // tracked since 40 days ago; in the last 30 days: 9 earlier points + now = 10, of which 8 with badge
        History h = new History().at(40, "500", "550", true)
                .at(27, "500", "550", true).at(24, "500", null, false).at(21, "500", "550", true)
                .at(18, "500", "550", true).at(15, "500", null, false).at(12, "500", "550", true)
                .at(9, "500", "550", true).at(6, "500", "550", true).at(3, "500", "550", true);
        Optional<Finding> finding = rule.evaluate(h.newestFirst(), h.now("500", "550", true));

        assertThat(finding).isPresent();
        assertThat(finding.get().verdict()).isEqualTo(Verdict.PERPETUAL_SALE);
        assertThat(finding.get().reason()).contains("8 of 10");
    }

    @Test
    void sevenOfTenIsNotEnough() {
        History h = new History().at(40, "500", "550", true)
                .at(27, "500", "550", true).at(24, "500", null, false).at(21, "500", null, false)
                .at(18, "500", "550", true).at(15, "500", null, false).at(12, "500", "550", true)
                .at(9, "500", "550", true).at(6, "500", "550", true).at(3, "500", "550", true);
        assertThat(rule.evaluate(h.newestFirst(), h.now("500", "550", true))).isEmpty();
    }

    @Test
    void fewerThanFiveObservationsInThePeriodIsNotEnough() {
        History h = new History().at(40, "500", "550", true).at(20, "500", "550", true)
                .at(10, "500", "550", true).at(5, "500", "550", true);
        assertThat(rule.evaluate(h.newestFirst(), h.now("500", "550", true))).isEmpty(); // 4 in last 30 days
    }

    @Test
    void productTrackedForLessThanThirtyDaysIsNotJudged() {
        History h = new History().every(4, 20, "500", "550", true);
        assertThat(rule.evaluate(h.newestFirst(), h.now("500", "550", true))).isEmpty();
    }
}
