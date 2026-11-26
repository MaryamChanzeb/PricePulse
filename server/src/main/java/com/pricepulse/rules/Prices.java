package com.pricepulse.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/** Small price helpers shared by the rules and the analysis service. */
public final class Prices {

    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Prices() {
    }

    /** 9999 -> "Rs 9,999"; 1500.5 -> "Rs 1,500.50" */
    public static String rs(BigDecimal amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ENGLISH);
        boolean whole = amount.stripTrailingZeros().scale() <= 0;
        DecimalFormat format = new DecimalFormat(whole ? "#,##0" : "#,##0.00", symbols);
        return "Rs " + format.format(amount);
    }

    /** (from - to) / from * 100, rounded to 2 decimals. Example: from 100, to 75 -> 25.00 */
    public static BigDecimal percentBelow(BigDecimal from, BigDecimal to) {
        return from.subtract(to).multiply(HUNDRED).divide(from, 2, RoundingMode.HALF_UP);
    }

    /** "16.67" -> "16.7%", "4.00" -> "4%" */
    public static String percentText(BigDecimal percent) {
        return percent.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    /** Middle value of the list (average of the two middle values for an even count). The list must not be empty. */
    public static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = new java.util.ArrayList<>(values);
        sorted.sort(null);
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return sorted.get(size / 2 - 1).add(sorted.get(size / 2)).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
