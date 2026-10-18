package com.eventtracker.service;

import com.eventtracker.model.Alert;
import com.eventtracker.model.AlertRule;
import com.eventtracker.model.Event;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.EventStatus;
import com.eventtracker.model.PaymentStatus;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.SponsorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private final EventRepository eventRepository;
    private final SponsorRepository sponsorRepository;

    public AlertService(EventRepository eventRepository, SponsorRepository sponsorRepository) {
        this.eventRepository = eventRepository;
        this.sponsorRepository = sponsorRepository;
    }

    public List<Alert> findAlerts() {
        return evaluate(eventRepository.findAllByOrderByEventDateAscIdAsc(),
                sponsorRepository.findAll(), LocalDate.now());
    }

    /**
     * Applies every {@link AlertRule} to the given data and returns the alerts with the most severe first
     * (the order within a severity is stable). Kept free of database access so it is easy to test.
     */
    static List<Alert> evaluate(List<Event> events, List<Sponsor> sponsors, LocalDate today) {
        List<Alert> alerts = new ArrayList<>();
        Set<Long> eventsWithActiveSponsors = new HashSet<>();

        for (Sponsor sponsor : sponsors) {
            SponsorshipStatus status = sponsor.getSponsorshipStatus();
            if (status == SponsorshipStatus.CANCELLED) {
                continue;
            }
            Event event = sponsor.getEvent();
            if (event != null) {
                eventsWithActiveSponsors.add(event.getId());
            }
            if (isBlank(sponsor.getContactPerson())) {
                alerts.add(Alert.forSponsor(AlertRule.SPONSOR_MISSING_CONTACT_PERSON, sponsor));
            }
            if (isBlank(sponsor.getEmail()) && isBlank(sponsor.getPhone())) {
                alerts.add(Alert.forSponsor(AlertRule.SPONSOR_NO_CONTACT_DETAILS, sponsor));
            }
            if (event == null) {
                alerts.add(Alert.forSponsor(AlertRule.SPONSOR_NOT_LINKED_TO_EVENT, sponsor));
            }
            if (status.isCommitted() && sponsor.getSponsorshipAmount() == null) {
                alerts.add(Alert.forSponsor(AlertRule.SPONSOR_COMMITTED_WITHOUT_AMOUNT, sponsor));
            }
            if (status.isOpen() && event != null
                    && EventStatus.of(event.getEventDate(), today) == EventStatus.PAST) {
                alerts.add(Alert.forSponsor(AlertRule.SPONSOR_UNDECIDED_FOR_PAST_EVENT, sponsor));
            }
            if (sponsor.paymentStatusOn(today) == PaymentStatus.OVERDUE) {
                alerts.add(Alert.forSponsor(AlertRule.SPONSOR_PAYMENT_OVERDUE, sponsor));
            }
        }

        Map<Long, EventFunding> funding = FundingCalculator.byEvent(events, sponsors);
        for (Event event : events) {
            if (EventStatus.of(event.getEventDate(), today) != EventStatus.UPCOMING) {
                continue;
            }
            if (!eventsWithActiveSponsors.contains(event.getId())) {
                alerts.add(Alert.forEvent(AlertRule.EVENT_WITHOUT_ACTIVE_SPONSORS, event));
            }
            EventFunding eventFunding = funding.get(event.getId());
            long daysUntil = ChronoUnit.DAYS.between(today, event.getEventDate());
            if (eventFunding.hasTarget() && daysUntil <= AlertRule.TARGET_WINDOW_DAYS
                    && eventFunding.committed().compareTo(eventFunding.target()) < 0) {
                alerts.add(Alert.forEvent(AlertRule.EVENT_BELOW_TARGET, event));
            }
        }
        alerts.sort(Comparator.comparing(alert -> alert.rule().getSeverity()));
        return alerts;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
