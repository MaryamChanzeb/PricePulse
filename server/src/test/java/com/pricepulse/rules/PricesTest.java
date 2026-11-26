package com.pricepulse.rules;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricesTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void medianOfOddAndEvenCounts() {
        assertThat(Prices.median(List.of(bd("300"), bd("100"), bd("200")))).isEqualByComparingTo("200");
        assertThat(Prices.median(List.of(bd("400"), bd("100"), bd("300"), bd("200")))).isEqualByComparingTo("250");
    }

    @Test
    void percentBelowAndText() {
        assertThat(Prices.percentBelow(bd("4199"), bd("3499"))).isEqualByComparingTo("16.67");
        assertThat(Prices.percentText(bd("16.67"))).isEqualTo("16.7%");
        assertThat(Prices.percentText(bd("4.00"))).isEqualTo("4%");
        assertThat(Prices.percentBelow(bd("1000"), bd("1100"))).isEqualByComparingTo("-10.00");
    }

    @Test
    void rupeesAreFormattedWithThousandSeparators() {
        assertThat(Prices.rs(bd("9999.00"))).isEqualTo("Rs 9,999");
        assertThat(Prices.rs(bd("1500.5"))).isEqualTo("Rs 1,500.50");
    }
}
