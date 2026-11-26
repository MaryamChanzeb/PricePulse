package com.pricepulse.rules;

import com.pricepulse.TestRules;
import com.pricepulse.model.Verdict;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-16: claimed original more than 10% above the highest price of the last 60 days. */
class InflatedOriginalPriceRuleTest {

    private final DiscountRule rule = new InflatedOriginalPriceRule(TestRules.defaults());

    @Test
    void noClaimedPriceMeansNothingToJudge() {
        History h = new History().at(20, "1000").at(10, "1000");
        assertThat(rule.evaluate(h.newestFirst(), h.now("900", null, true))).isEmpty();
    }

    @Test
    void exactlyTenPercentAboveIsNotInflated() {
        History h = new History().at(20, "1000").at(10, "1000");
        assertThat(rule.evaluate(h.newestFirst(), h.now("900", "1100", true))).isEmpty();
    }

    @Test
    void justOverTenPercentIsInflated() {
        History h = new History().at(20, "1000").at(10, "1000");
        Optional<Finding> finding = rule.evaluate(h.newestFirst(), h.now("900", "1100.01", true));

        assertThat(finding).isPresent();
        assertThat(finding.get().verdict()).isEqualTo(Verdict.INFLATED_ORIGINAL);
        assertThat(finding.get().reason()).contains("Rs 1,100.01").contains("Rs 1,000").contains("60 days");
    }

    @Test
    void higherPriceOlderThanSixtyDaysDoesNotCount() {
        History h = new History().at(70, "2000").at(30, "1000").at(10, "1000");
        assertThat(rule.evaluate(h.newestFirst(), h.now("900", "1500", true))).isPresent();
    }

    @Test
    void higherPriceInsideSixtyDaysMakesTheClaimBelievable() {
        History h = new History().at(59, "1400").at(30, "1000").at(10, "1000");
        assertThat(rule.evaluate(h.newestFirst(), h.now("900", "1500", true))).isEmpty(); // 1500 <= 1540
    }
}
