package dev.nametags;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberFormatUtil {
    private static final String[] SUFFIX = {"", "K", "M", "B", "T", "Q"};
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private NumberFormatUtil() {}

    /** 1123 -> 1.12K, 1000 -> 1K, 1500 -> 1.5K, 1000000 -> 1M */
    public static String format(double amount) {
        int idx = 0;
        BigDecimal v = BigDecimal.valueOf(amount);
        while (v.abs().compareTo(THOUSAND) >= 0 && idx < SUFFIX.length - 1) {
            v = v.divide(THOUSAND);
            idx++;
        }
        v = v.setScale(2, RoundingMode.HALF_UP);
        if (v.abs().compareTo(THOUSAND) >= 0 && idx < SUFFIX.length - 1) { // 999.999K -> 1M
            v = v.divide(THOUSAND).setScale(2, RoundingMode.HALF_UP);
            idx++;
        }
        if (v.signum() == 0) return "0";
        return v.stripTrailingZeros().toPlainString() + SUFFIX[idx];
    }
}
