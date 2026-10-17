package com.eventtracker.service;

import com.eventtracker.model.DashboardSummary;
import com.eventtracker.model.Event;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.EventStatus;
import com.eventtracker.model.PaymentStatus;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.StatusSummary;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.SponsorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.eventtracker.service.FundingCalculator.totalAmount;
import static com.eventtracker.service.FundingCalculator.totalPaid;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EventRepository eventRepository;
    private final SponsorRepository sponsorRepository;

    public DashboardService(EventRepository eventRepository, SponsorRepository sponsorRepository) {
        this.eventRepository = eventRepository;
        this.sponsorRepository = sponsorRepository;
    }

    public DashboardSummary getSummary() {
        List<Event> events = eventRepository.findAllByOrderByEventDateAscIdAsc();
        List<Sponsor> sponsors = sponsorRepository.findAll();
        return summarize(events, sponsors, LocalDate.now());
    }

    /** Funding progress of the next upcoming events (soonest first), for the dashboard panel. */
    public List<EventFunding> upcomingFunding(int limit) {
        LocalDate today = LocalDate.now();
        List<Event> upcoming = eventRepository.findAllByOrderByEventDateAscIdAsc().stream()
                .filter(event -> EventStatus.of(event.getEventDate(), today) == EventStatus.UPCOMING)
                .limit(limit)
                .toList();
        return List.copyOf(FundingCalculator.byEvent(upcoming, sponsorRepository.findAll()).values());
    }

    /** Calculates the dashboard indicators. Kept free of database access so it is easy to test. */
    static DashboardSummary summarize(List<Event> events, List<Sponsor> sponsors, LocalDate today) {
        List<Event> upcomingEvents = events.stream()
                .filter(event -> EventStatus.of(event.getEventDate(), today) == EventStatus.UPCOMING)
                .toList();
        long upcoming = upcomingEvents.size();

        List<StatusSummary> byStatus = new ArrayList<>();
        for (SponsorshipStatus status : SponsorshipStatus.values()) {
            List<Sponsor> matching = sponsors.stream()
                    .filter(sponsor -> sponsor.getSponsorshipStatus() == status)
                    .toList();
            byStatus.add(new StatusSummary(status, matching.size(), totalAmount(matching)));
        }

        List<Sponsor> committedSponsors = sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus().isCommitted())
                .toList();
        BigDecimal committed = totalAmount(committedSponsors);
        BigDecimal collected = totalPaid(committedSponsors);
        BigDecimal open = totalAmount(sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus().isOpen())
                .toList());
        long withoutAmount = sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipAmount() == null)
                .count();
        long overdue = sponsors.stream()
                .filter(sponsor -> sponsor.paymentStatusOn(today) == PaymentStatus.OVERDUE)
                .count();
        long alertCount = AlertService.evaluate(events, sponsors, today).size();

        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal upcomingCommitted = BigDecimal.ZERO;
        for (EventFunding funding : FundingCalculator.byEvent(upcomingEvents, sponsors).values()) {
            if (funding.hasTarget()) {
                totalTarget = totalTarget.add(funding.target());
                upcomingCommitted = upcomingCommitted.add(funding.committed());
            }
        }

        return new DashboardSummary(events.size(), upcoming, events.size() - upcoming, sponsors.size(),
                committed, open, withoutAmount, alertCount, byStatus,
                collected, committed.subtract(collected).max(BigDecimal.ZERO), overdue,
                totalTarget, upcomingCommitted, FundingCalculator.byTier(sponsors));
    }
}
