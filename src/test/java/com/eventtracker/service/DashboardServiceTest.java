package com.eventtracker.service;

import com.eventtracker.model.DashboardSummary;
import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.StatusSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    @Test
    void emptyDataGivesZeroIndicators() {
        DashboardSummary summary = DashboardService.summarize(List.of(), List.of(), TODAY);

        assertThat(summary.totalEvents()).isZero();
        assertThat(summary.totalSponsors()).isZero();
        assertThat(summary.committedAmount()).isEqualByComparingTo("0");
        assertThat(summary.openAmount()).isEqualByComparingTo("0");
        assertThat(summary.alertCount()).isZero();
        assertThat(summary.sponsorsByStatus()).hasSize(SponsorshipStatus.values().length)
                .allSatisfy(row -> assertThat(row.count()).isZero());
    }

    @Test
    void countsEventsAndTotalsAmountsByStatus() {
        Event upcoming = event(1L, TODAY.plusDays(3));
        Event past = event(2L, TODAY.minusDays(3));
        List<Sponsor> sponsors = List.of(
                sponsor(10L, SponsorshipStatus.CONFIRMED, "10000", upcoming),
                sponsor(11L, SponsorshipStatus.COMPLETED, "5000.50", past),
                sponsor(12L, SponsorshipStatus.PROPOSED, "2000", upcoming),
                sponsor(13L, SponsorshipStatus.PENDING, null, upcoming),
                sponsor(14L, SponsorshipStatus.CANCELLED, "999", upcoming));

        DashboardSummary summary = DashboardService.summarize(List.of(upcoming, past), sponsors, TODAY);

        assertThat(summary.totalEvents()).isEqualTo(2);
        assertThat(summary.upcomingEvents()).isEqualTo(1);
        assertThat(summary.pastEvents()).isEqualTo(1);
        assertThat(summary.totalSponsors()).isEqualTo(5);
        assertThat(summary.committedAmount()).isEqualByComparingTo("15000.50");
        assertThat(summary.openAmount()).isEqualByComparingTo("2000");
        assertThat(summary.sponsorsWithoutAmount()).isEqualTo(1);
        assertThat(row(summary, SponsorshipStatus.CANCELLED).count()).isEqualTo(1);
        assertThat(row(summary, SponsorshipStatus.CANCELLED).totalAmount()).isEqualByComparingTo("999");
        assertThat(row(summary, SponsorshipStatus.PENDING).totalAmount()).isEqualByComparingTo("0");
    }

    private static StatusSummary row(DashboardSummary summary, SponsorshipStatus status) {
        return summary.sponsorsByStatus().stream()
                .filter(row -> row.status() == status)
                .findFirst()
                .orElseThrow();
    }

    private static Event event(Long id, LocalDate date) {
        Event event = new Event("Event " + id, date, "Venue", null);
        event.setId(id);
        return event;
    }

    private static Sponsor sponsor(Long id, SponsorshipStatus status, String amount, Event event) {
        Sponsor sponsor = new Sponsor("Sponsor " + id, status);
        sponsor.setId(id);
        sponsor.setContactPerson("Contact");
        sponsor.setEmail("contact@example.com");
        sponsor.setSponsorshipAmount(amount == null ? null : new BigDecimal(amount));
        sponsor.setEvent(event);
        return sponsor;
    }
}
