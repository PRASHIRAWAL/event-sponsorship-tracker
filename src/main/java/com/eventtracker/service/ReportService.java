package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.PaymentMethod;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.TierSummary;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.PaymentRepository;
import com.eventtracker.repository.SponsorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Read-only figures for the Reports page, its API and the CSV exports. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    /** Totals received per payment method. */
    public record MethodTotal(PaymentMethod method, long count, BigDecimal amount) {
    }

    private final EventRepository eventRepository;
    private final SponsorRepository sponsorRepository;
    private final PaymentRepository paymentRepository;

    public ReportService(EventRepository eventRepository, SponsorRepository sponsorRepository,
                         PaymentRepository paymentRepository) {
        this.eventRepository = eventRepository;
        this.sponsorRepository = sponsorRepository;
        this.paymentRepository = paymentRepository;
    }

    /** Funding for every event, in date order. */
    public List<EventFunding> eventFunding() {
        return List.copyOf(FundingCalculator.byEvent(eventRepository.findAllByOrderByEventDateAscIdAsc(),
                sponsorRepository.findAll()).values());
    }

    /** Funding for the given events only (used by the paged event list). */
    public Map<Long, EventFunding> fundingFor(List<Event> events) {
        return FundingCalculator.byEvent(events, sponsorRepository.findAll());
    }

    public EventFunding fundingFor(Event event) {
        return FundingCalculator.forEvent(event, sponsorRepository.findAll().stream()
                .filter(sponsor -> sponsor.getEvent() != null
                        && Objects.equals(sponsor.getEvent().getId(), event.getId()))
                .toList());
    }

    public List<TierSummary> tierBreakdown() {
        return FundingCalculator.byTier(sponsorRepository.findAll());
    }

    /** Every payment method, including ones with no payments, in declaration order. */
    public List<MethodTotal> paymentMethods() {
        Map<PaymentMethod, MethodTotal> totals = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            totals.put(method, new MethodTotal(method, 0, BigDecimal.ZERO));
        }
        for (Object[] row : paymentRepository.totalsByMethod()) {
            PaymentMethod method = (PaymentMethod) row[0];
            totals.put(method, new MethodTotal(method, ((Number) row[2]).longValue(), (BigDecimal) row[1]));
        }
        return new ArrayList<>(totals.values());
    }

    /** The largest committed sponsorships. */
    public List<Sponsor> topSponsors(int limit) {
        return sponsorRepository.findAll().stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus() != SponsorshipStatus.CANCELLED)
                .filter(sponsor -> sponsor.getSponsorshipAmount() != null)
                .sorted(Comparator.comparing(Sponsor::getSponsorshipAmount).reversed()
                        .thenComparing(Sponsor::getName))
                .limit(limit)
                .toList();
    }
}
