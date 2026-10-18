package com.eventtracker.controller;

import com.eventtracker.model.StatusSummary;
import com.eventtracker.model.TierSummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Builds CSS for the dashboard and report charts, which are drawn with plain CSS (no chart library):
 * donuts are a conic-gradient, bars are widths in percent.
 */
public final class Charts {

    private Charts() {
    }

    /** conic-gradient for sponsor counts by status, using the --c-{status} colour tokens. */
    public static String statusDonut(List<StatusSummary> rows) {
        long total = rows.stream().mapToLong(StatusSummary::count).sum();
        StringBuilder css = new StringBuilder("background: conic-gradient(");
        double start = 0;
        boolean first = true;
        for (StatusSummary row : rows) {
            if (row.count() == 0) {
                continue;
            }
            double end = start + row.count() * 100.0 / total;
            first = appendSegment(css, first, "--c-" + row.status().name().toLowerCase(Locale.ROOT), start, end);
            start = end;
        }
        return finish(css, first);
    }

    /** conic-gradient for active sponsors by tier, using the --c-{tier} colour tokens. */
    public static String tierDonut(List<TierSummary> rows) {
        long total = rows.stream().mapToLong(TierSummary::count).sum();
        StringBuilder css = new StringBuilder("background: conic-gradient(");
        double start = 0;
        boolean first = true;
        for (TierSummary row : rows) {
            if (row.count() == 0) {
                continue;
            }
            double end = start + row.count() * 100.0 / total;
            first = appendSegment(css, first, "--c-" + row.cssKey(), start, end);
            start = end;
        }
        return finish(css, first);
    }

    /** Bar width (0-100) of a value relative to the largest value in the chart. */
    public static int width(BigDecimal value, BigDecimal max) {
        if (value == null || max == null || max.signum() <= 0) {
            return 0;
        }
        return value.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue();
    }

    private static boolean appendSegment(StringBuilder css, boolean first, String token, double start, double end) {
        if (!first) {
            css.append(", ");
        }
        css.append(String.format(Locale.ROOT, "var(%s) %.2f%% %.2f%%", token, start, end));
        return false;
    }

    private static String finish(StringBuilder css, boolean empty) {
        if (empty) {
            return "";
        }
        return css.append(")").toString();
    }
}
