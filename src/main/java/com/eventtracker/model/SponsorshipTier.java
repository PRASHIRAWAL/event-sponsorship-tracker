package com.eventtracker.model;

/** Sponsorship package a sponsor has bought. Ordered from the highest to the lowest package. */
public enum SponsorshipTier {

    PLATINUM("Platinum"),
    GOLD("Gold"),
    SILVER("Silver"),
    BRONZE("Bronze"),
    COMMUNITY("Community");

    private final String label;

    SponsorshipTier(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
