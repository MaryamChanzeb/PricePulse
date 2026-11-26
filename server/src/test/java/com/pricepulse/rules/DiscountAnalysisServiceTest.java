package com.pricepulse.rules;

import com.pricepulse.TestRules;
import com.pricepulse.model.Verdict;
import com.pricepulse.service.DiscountAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The six prepared price histories (one per verdict, plus a tie between two rules) run through the
 * real rule list, exactly as DiscountAnalysisService uses them. FR-15 to FR-21. No database.
 */
class DiscountAnalysisServiceTest {

    private final RuleConfig config = TestRules.defaults();
    private final DiscountAnalysisService service = new DiscountAnalysisService(
            List.of(new InflatedOriginalPriceRule(config), new PreSaleHikeRule(config), new PerpetualSaleRule(config)),
            config, null, null);

    @Test
    void history1GenuineDiscount() {
        History h = new History().every(5, 40, "1000", null, false);
        VerdictResult result = service.evaluate(h.newestFirst(), h.now("800", "1000", true));

        assertThat(result.verdict()).isEqualTo(Verdict.GENUINE);
        assertThat(result.realDiscountPct()).isEqualByComparingTo("20.00");   // median 1000 -> 800
        assertThat(result.claimedDiscountPct()).isEqualByComparingTo("20.00");
        assertThat(result.reason()).contains("20% below the 30-day median of Rs 1,000");
    }

    @Test
    void history2InflatedOriginalPrice() {
        History h = new History().every(5, 40, "1000", null, false);
        VerdictResult result = service.evaluate(h.newestFirst(), h.now("950", "2000", true));

        assertThat(result.verdict()).isEqualTo(Verdict.INFLATED_ORIGINAL);
        assertThat(result.claimedDiscountPct()).isEqualByComparingTo("52.50");
        assertThat(result.realDiscountPct()).isEqualByComparingTo("5.00");
    }

    @Test
    void history3PriceHikeBeforeSale() {
        History h = new History().at(40, "1000").at(35, "1000").at(30, "1000").at(25, "1000")
                .at(20, "1000").at(16, "1000")                       // normal price
                .at(12, "1200").at(8, "1200").at(3, "1200");         // raised 12 days ago
        VerdictResult result = service.evaluate(h.newestFirst(), h.now("1000", "1200", true));

        assertThat(result.verdict()).isEqualTo(Verdict.PRICE_HIKE_BEFORE_SALE);
    }

    @Test
    void history4PerpetualSale() {
        History h = new History().every(4, 40, "500", "520", true); // "sale" every time for 40 days
        VerdictResult result = service.evaluate(h.newestFirst(), h.now("500", "520", true));

        assertThat(result.verdict()).isEqualTo(Verdict.PERPETUAL_SALE);
    }

    @Test
    void history5NotEnoughData() {
        History fewPoints = new History().at(10, "1000");
        assertThat(service.evaluate(fewPoints.newestFirst(), fewPoints.now("900", "1000", true)).verdict())
                .isEqualTo(Verdict.NOT_ENOUGH_DATA);

        History shortSpan = new History().at(5, "1000").at(3, "1000").at(1, "1000"); // 4 points but only 5 days
        VerdictResult result = service.evaluate(shortSpan.newestFirst(), shortSpan.now("900", "1000", true));
        assertThat(result.verdict()).isEqualTo(Verdict.NOT_ENOUGH_DATA);
        assertThat(result.realDiscountPct()).isNull();
        assertThat(result.reason()).contains("4 observation(s) over 5 day(s)");
    }

    @Test
    void history6TieBetweenTwoRulesHighestPriorityWins() {
        // Always on "sale" (perpetual) AND the claimed price was never charged (inflated): FR-20 -> INFLATED_ORIGINAL
        History h = new History().every(4, 40, "500", "900", true);
        VerdictResult result = service.evaluate(h.newestFirst(), h.now("500", "900", true));

        assertThat(result.verdict()).isEqualTo(Verdict.INFLATED_ORIGINAL);
    }

    @Test
    void exactlyThreeObservationsSevenDaysApartIsEnough() {
        History h = new History().at(7, "1000").at(3, "1000");
        assertThat(service.evaluate(h.newestFirst(), h.now("1000", null, false)).verdict())
                .isEqualTo(Verdict.GENUINE);
    }
}
