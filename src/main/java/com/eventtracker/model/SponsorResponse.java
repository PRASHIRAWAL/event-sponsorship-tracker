package com.eventtracker.model;

import java.math.BigDecimal;

/** JSON representation of a sponsor returned by the REST API. */
public record SponsorResponse(
        Long id,
        String name,
        String contactPerson,
        String email,
        String phone,
        BigDecimal sponsorshipAmount,
        SponsorshipStatus sponsorshipStatus,
        Long eventId,
        String eventName) {

    public static SponsorResponse from(Sponsor sponsor) {
        Event event = sponsor.getEvent();
        return new SponsorResponse(
                sponsor.getId(),
                sponsor.getName(),
                sponsor.getContactPerson(),
                sponsor.getEmail(),
                sponsor.getPhone(),
                sponsor.getSponsorshipAmount(),
                sponsor.getSponsorshipStatus(),
                event == null ? null : event.getId(),
                event == null ? null : event.getName());
    }
}
