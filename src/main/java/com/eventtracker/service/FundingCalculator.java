package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import com.eventtracker.model.TierSummary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure funding calculations shared by the dashboard, alerts, event pages and reports. */
public final class FundingCalculator {

    private FundingCalculator() {
    }

    /** Funding for every given event, keyed by event id, in the order of the event list. */
    public static Map<Long, EventFunding> byEvent(List<Event> events, List<Sponsor> sponsors) {
        Map<Long, EventFunding> result = new LinkedHashMap<>();
        for (Event event : events) {
            List<Sponsor> linked = sponsors.stream()
                    .filter(sponsor -> sponsor.getEvent() != null
                            && Objects.equals(sponsor.getEvent().getId(), event.getId()))
                    .toList();
            result.put(event.getId(), forEvent(event, linked));
        }
        return result;
    }

    /** Funding for one event from the sponsors linked to it. */
    public static EventFunding forEvent(Event event, List<Sponsor> linkedSponsors) {
        List<Sponsor> committed = linkedSponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus().isCommitted())
                .toList();
        List<Sponsor> open = linkedSponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus().isOpen())
                .toList();
        long active = linkedSponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus() != SponsorshipStatus.CANCELLED)
                .count();
        return new EventFunding(event, totalAmount(committed), totalAmount(open), totalPaid(committed), active);
    }

    /** One row per tier (highest first) plus "No tier"; cancelled sponsorships are left out. */
    public static List<TierSummary> byTier(List<Sponsor> sponsors) {
        List<Sponsor> active = sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus() != SponsorshipStatus.CANCELLED)
                .toList();
        List<TierSummary> rows = new ArrayList<>();
        for (SponsorshipTier tier : SponsorshipTier.values()) {
            rows.add(tierRow(tier, active));
        }
        rows.add(tierRow(null, active));
        return rows;
    }

    private static TierSummary tierRow(SponsorshipTier tier, List<Sponsor> sponsors) {
        List<Sponsor> matching = sponsors.stream().filter(sponsor -> sponsor.getTier() == tier).toList();
        return new TierSummary(tier, matching.size(), totalAmount(matching));
    }

    public static BigDecimal totalAmount(List<Sponsor> sponsors) {
        return sponsors.stream()
                .map(Sponsor::getSponsorshipAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal totalPaid(List<Sponsor> sponsors) {
        return sponsors.stream()
                .map(Sponsor::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
