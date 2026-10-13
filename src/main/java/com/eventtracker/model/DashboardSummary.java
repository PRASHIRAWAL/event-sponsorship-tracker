package com.eventtracker.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard indicators. Amount totals only include sponsors that have an amount recorded.
 *
 * @param committedAmount total of Confirmed and Completed sponsorships
 * @param openAmount      total of Proposed and Pending sponsorships (not yet decided)
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
        List<StatusSummary> sponsorsByStatus) {
}
