package com.eventtracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** JSON representation of a sponsor returned by the REST API. */
public record SponsorResponse(
        Long id,
        String name,
        String contactPerson,
        String email,
        String phone,
        String industry,
        BigDecimal sponsorshipAmount,
        SponsorshipStatus sponsorshipStatus,
        SponsorshipTier tier,
        LocalDate paymentDueDate,
        BigDecimal amountPaid,
        BigDecimal outstandingAmount,
        PaymentStatus paymentStatus,
        String notes,
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
                sponsor.getIndustry(),
                sponsor.getSponsorshipAmount(),
                sponsor.getSponsorshipStatus(),
                sponsor.getTier(),
                sponsor.getPaymentDueDate(),
                sponsor.getAmountPaid(),
                sponsor.getOutstandingAmount(),
                sponsor.getPaymentStatus(),
                sponsor.getNotes(),
                event == null ? null : event.getId(),
                event == null ? null : event.getName());
    }
}
