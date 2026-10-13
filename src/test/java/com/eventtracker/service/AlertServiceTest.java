package com.eventtracker.service;

import com.eventtracker.model.Alert;
import com.eventtracker.model.AlertRule;
import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    @Test
    void completeSponsorForUpcomingEventRaisesNoAlerts() {
        Event event = event(1L, TODAY.plusDays(10));
        Sponsor sponsor = completeSponsor(10L, SponsorshipStatus.CONFIRMED, event);

        assertThat(AlertService.evaluate(List.of(event), List.of(sponsor), TODAY)).isEmpty();
    }

    @Test
    void sponsorWithoutContactDetailsOrEventIsFlagged() {
        Sponsor sponsor = new Sponsor("Acme Corp", SponsorshipStatus.PROPOSED);
        sponsor.setId(10L);

        assertThat(rules(AlertService.evaluate(List.of(), List.of(sponsor), TODAY)))
                .containsExactlyInAnyOrder(
                        AlertRule.SPONSOR_MISSING_CONTACT_PERSON,
                        AlertRule.SPONSOR_NO_CONTACT_DETAILS,
                        AlertRule.SPONSOR_NOT_LINKED_TO_EVENT);
    }

    @Test
    void phoneAloneCountsAsContactDetails() {
        Event event = event(1L, TODAY.plusDays(10));
        Sponsor sponsor = completeSponsor(10L, SponsorshipStatus.CONFIRMED, event);
        sponsor.setEmail(null);

        assertThat(AlertService.evaluate(List.of(event), List.of(sponsor), TODAY)).isEmpty();
    }

    @Test
    void committedSponsorshipWithoutAmountIsFlagged() {
        Event event = event(1L, TODAY.plusDays(10));
        Sponsor sponsor = completeSponsor(10L, SponsorshipStatus.COMPLETED, event);
        sponsor.setSponsorshipAmount(null);

        assertThat(rules(AlertService.evaluate(List.of(event), List.of(sponsor), TODAY)))
                .containsExactly(AlertRule.SPONSOR_COMMITTED_WITHOUT_AMOUNT);
    }

    @Test
    void proposedSponsorshipWithoutAmountIsNotFlagged() {
        Event event = event(1L, TODAY.plusDays(10));
        Sponsor sponsor = completeSponsor(10L, SponsorshipStatus.PROPOSED, event);
        sponsor.setSponsorshipAmount(null);

        assertThat(AlertService.evaluate(List.of(event), List.of(sponsor), TODAY)).isEmpty();
    }

    @Test
    void undecidedSponsorshipForPastEventIsFlagged() {
        Event pastEvent = event(1L, TODAY.minusDays(1));
        Sponsor sponsor = completeSponsor(10L, SponsorshipStatus.PENDING, pastEvent);

        assertThat(rules(AlertService.evaluate(List.of(pastEvent), List.of(sponsor), TODAY)))
                .containsExactly(AlertRule.SPONSOR_UNDECIDED_FOR_PAST_EVENT);
    }

    @Test
    void eventTodayCountsAsUpcomingAndNeedsSponsors() {
        Event today = event(1L, TODAY);

        List<Alert> alerts = AlertService.evaluate(List.of(today), List.of(), TODAY);

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.rule()).isEqualTo(AlertRule.EVENT_WITHOUT_ACTIVE_SPONSORS);
            assertThat(alert.subjectType()).isEqualTo(Alert.EVENT);
            assertThat(alert.subjectId()).isEqualTo(1L);
        });
    }

    @Test
    void upcomingEventWithOnlyCancelledSponsorsIsFlagged() {
        Event event = event(1L, TODAY.plusDays(5));
        Sponsor cancelled = new Sponsor("Gone Ltd", SponsorshipStatus.CANCELLED);
        cancelled.setId(10L);
        cancelled.setEvent(event);

        assertThat(rules(AlertService.evaluate(List.of(event), List.of(cancelled), TODAY)))
                .containsExactly(AlertRule.EVENT_WITHOUT_ACTIVE_SPONSORS);
    }

    @Test
    void pastEventWithoutSponsorsIsNotFlagged() {
        assertThat(AlertService.evaluate(List.of(event(1L, TODAY.minusDays(30))), List.of(), TODAY)).isEmpty();
    }

    private static Event event(Long id, LocalDate date) {
        Event event = new Event("Event " + id, date, "Main Auditorium", null);
        event.setId(id);
        return event;
    }

    private static Sponsor completeSponsor(Long id, SponsorshipStatus status, Event event) {
        Sponsor sponsor = new Sponsor("Sponsor " + id, status);
        sponsor.setId(id);
        sponsor.setContactPerson("Jane Doe");
        sponsor.setEmail("jane@example.com");
        sponsor.setPhone("9876543210");
        sponsor.setSponsorshipAmount(new BigDecimal("10000"));
        sponsor.setEvent(event);
        return sponsor;
    }

    private static List<AlertRule> rules(List<Alert> alerts) {
        return alerts.stream().map(Alert::rule).toList();
    }
}
