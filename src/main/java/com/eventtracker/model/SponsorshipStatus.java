package com.eventtracker.model;

public enum SponsorshipStatus {

    PROPOSED("Proposed"),
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    SponsorshipStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Confirmed and completed sponsorships count towards the committed sponsorship total. */
    public boolean isCommitted() {
        return this == CONFIRMED || this == COMPLETED;
    }

    /** Proposed and pending sponsorships still need a decision. */
    public boolean isOpen() {
        return this == PROPOSED || this == PENDING;
    }
}
