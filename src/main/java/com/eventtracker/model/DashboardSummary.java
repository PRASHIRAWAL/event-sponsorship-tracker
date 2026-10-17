package com.eventtracker.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard indicators. Amount totals only include sponsors that have an amount recorded.
 *
 * @param committedAmount   total of Confirmed and Completed sponsorships
 * @param openAmount        total of Proposed and Pending sponsorships (not yet decided)
 * @param collectedAmount   payments received from Confirmed and Completed sponsors
 * @param outstandingAmount committed amount not yet received
 * @param overduePayments   sponsors whose payment status is Overdue
 * @param totalTarget       sum of the sponsorship targets of upcoming events
 * @param upcomingCommitted committed amount for upcoming events that have a target
 */
public record DashboardSummary(
        long totalEvents,
        long upcomingEvents,
        long pastEvents,
        long totalSponsors,
        BigDecimal committedAmount,
        BigDecimal openAmount,
        long sponsorsWithoutAmount,
        long alertCount,
        List<StatusSummary> sponsorsByStatus,
        BigDecimal collectedAmount,
        BigDecimal outstandingAmount,
        long overduePayments,
        BigDecimal totalTarget,
        BigDecimal upcomingCommitted,
        List<TierSummary> sponsorsByTier) {

    /** Share of the committed amount already received (0-100). */
    public int collectionRate() {
        return percent(collectedAmount, committedAmount);
    }

    /** Committed amount for upcoming events as a share of their targets (0-100). */
    public int targetProgress() {
        return percent(upcomingCommitted, totalTarget);
    }

    /** Bar width (0-100) for a tier row, relative to the tier with the most sponsors. */
    public int tierWidth(TierSummary row) {
        long max = sponsorsByTier.stream().mapToLong(TierSummary::count).max().orElse(0);
        return max == 0 ? 0 : (int) (row.count() * 100 / max);
    }

    private static int percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() <= 0) {
            return 0;
        }
        int value = part.multiply(BigDecimal.valueOf(100)).divide(whole, 0, java.math.RoundingMode.DOWN).intValue();
        return Math.min(100, value);
    }
}
