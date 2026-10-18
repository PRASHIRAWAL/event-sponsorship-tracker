package com.eventtracker.model;

/** Search criteria for the sponsor list; null or blank values mean "no filter". */
public record SponsorFilter(String keyword, SponsorshipStatus status, Long eventId, SponsorshipTier tier,
                            PaymentStatus paymentStatus) {

    public static SponsorFilter none() {
        return new SponsorFilter(null, null, null, null, null);
    }

    public boolean isActive() {
        return (keyword != null && !keyword.isBlank()) || status != null || eventId != null
                || tier != null || paymentStatus != null;
    }
}
