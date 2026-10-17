package com.eventtracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment status is derived from the sponsorship amount, the amount paid so far and the payment due date.
 * It is not stored in the database, so it always reflects the current payments and today's date.
 */
public enum PaymentStatus {

    UNPAID("Unpaid"),
    PARTIAL("Partially paid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    NOT_APPLICABLE("No amount");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Statuses a user can filter by (NOT_APPLICABLE is shown, but not offered as a filter). */
    public static PaymentStatus[] filterable() {
        return new PaymentStatus[] {UNPAID, PARTIAL, PAID, OVERDUE};
    }

    /**
     * Cancelled sponsorships and sponsorships without an amount have nothing to collect (NOT_APPLICABLE).
     * A fully paid sponsorship is PAID even after its due date; any other balance after the due date is OVERDUE.
     */
    public static PaymentStatus of(SponsorshipStatus status, BigDecimal amount, BigDecimal paid,
                                   LocalDate dueDate, LocalDate today) {
        if (status == SponsorshipStatus.CANCELLED || amount == null) {
            return NOT_APPLICABLE;
        }
        BigDecimal alreadyPaid = paid == null ? BigDecimal.ZERO : paid;
        if (alreadyPaid.compareTo(amount) >= 0) {
            return PAID;
        }
        if (dueDate != null && dueDate.isBefore(today)) {
            return OVERDUE;
        }
        return alreadyPaid.signum() > 0 ? PARTIAL : UNPAID;
    }
}
