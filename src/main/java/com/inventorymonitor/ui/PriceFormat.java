package com.inventorymonitor.ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Whole-shilling formatting shared by tables and product forms. */
public final class PriceFormat {
    private PriceFormat() {}

    public static String format(long value) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

    public static long parse(String input) {
        String value = input == null ? "" : input.trim();
        if (!value.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)")) {
            throw new IllegalArgumentException("Enter a whole price, such as 15000 or 15,000.");
        }
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Price is too large.");
        }
    }
}
