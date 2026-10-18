package com.eventtracker.model;

import java.math.BigDecimal;

/** Number of sponsors and total recorded amount for one sponsorship status. */
public record StatusSummary(SponsorshipStatus status, long count, BigDecimal totalAmount) {
}
