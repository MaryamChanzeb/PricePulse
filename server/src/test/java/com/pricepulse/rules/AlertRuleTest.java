package com.pricepulse.rules;

import com.pricepulse.TestRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** SDS Figure 4: both alert rules used through the AlertRule parent type. */
class AlertRuleTest {

    private final AlertRule target = new TargetPriceAlertRule();
    private final AlertRule drop = new PercentageDropAlertRule(TestRules.defaults()); // 20%

    private static BigDecimal rs(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Test
    void targetRuleFiresAtOrBelowTheTarget() {
        assertThat(target.isTriggered(rs("5500"), rs("5000"), rs("5000"))).isTrue();   // exactly at target
        assertThat(target.isTriggered(rs("5500"), rs("4999"), rs("5000"))).isTrue();
        assertThat(target.isTriggered(rs("5500"), rs("5000.01"), rs("5000"))).isFalse();
        assertThat(target.message("Smart Watch X2", rs("5500"), rs("4999"), rs("5000")))
                .isEqualTo("Smart Watch X2 is now Rs 4,999, at or below your target of Rs 5,000.");
    }

    @Test
    void dropRuleFiresForAFallOfAtLeastTwentyPercent() {
        assertThat(drop.isTriggered(rs("1000"), rs("800"), rs("500"))).isTrue();    // exactly 20%
        assertThat(drop.isTriggered(rs("1000"), rs("800.01"), rs("500"))).isFalse(); // 19.999%
        assertThat(drop.isTriggered(rs("1000"), rs("1200"), rs("500"))).isFalse();   // went up
        assertThat(drop.isTriggered(null, rs("100"), rs("500"))).isFalse();          // no earlier price
        assertThat(drop.message("Air Fryer", rs("10000"), rs("7500"), rs("6000")))
                .isEqualTo("Air Fryer dropped 25% from Rs 10,000 to Rs 7,500 (your target is Rs 6,000).");
    }
}
