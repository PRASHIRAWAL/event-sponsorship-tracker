package com.eventtracker.model;

import java.math.BigDecimal;
import java.util.Locale;

/** Active (not cancelled) sponsors and their recorded amount for one tier; tier is null for "No tier". */
public record TierSummary(SponsorshipTier tier, long count, BigDecimal totalAmount) {

    public String label() {
        return tier == null ? "No tier" : tier.getLabel();
    }

    public String cssKey() {
        return tier == null ? "none" : tier.name().toLowerCase(Locale.ROOT);
    }
}
