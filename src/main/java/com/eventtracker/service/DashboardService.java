package com.eventtracker.service;

import com.eventtracker.model.DashboardSummary;
import com.eventtracker.model.Event;
import com.eventtracker.model.EventStatus;
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

    /** Calculates the dashboard indicators. Kept free of database access so it is easy to test. */
    static DashboardSummary summarize(List<Event> events, List<Sponsor> sponsors, LocalDate today) {
        long upcoming = events.stream()
                .filter(event -> EventStatus.of(event.getEventDate(), today) == EventStatus.UPCOMING)
                .count();

        List<StatusSummary> byStatus = new ArrayList<>();
        for (SponsorshipStatus status : SponsorshipStatus.values()) {
            List<Sponsor> matching = sponsors.stream()
                    .filter(sponsor -> sponsor.getSponsorshipStatus() == status)
                    .toList();
            byStatus.add(new StatusSummary(status, matching.size(), totalAmount(matching)));
        }

        BigDecimal committed = totalAmount(sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus().isCommitted())
                .toList());
        BigDecimal open = totalAmount(sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus().isOpen())
                .toList());
        long withoutAmount = sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipAmount() == null)
                .count();
        long alertCount = AlertService.evaluate(events, sponsors, today).size();

        return new DashboardSummary(events.size(), upcoming, events.size() - upcoming, sponsors.size(),
                committed, open, withoutAmount, alertCount, byStatus);
    }

    private static BigDecimal totalAmount(List<Sponsor> sponsors) {
        return sponsors.stream()
                .map(Sponsor::getSponsorshipAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
