package com.eventtracker.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formats amounts the same way the pages do (12,345.00), for activity messages and exports. */
public final class Money {

    private Money() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return format.format(amount);
    }
}
