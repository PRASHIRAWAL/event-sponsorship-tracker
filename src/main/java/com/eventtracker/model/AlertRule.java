package com.eventtracker.model;

/**
 * The rules used to raise alerts. Each rule is deliberately simple and is shown on the Alerts page
 * so users can see exactly why an item was flagged. Cancelled sponsorships never raise alerts.
 */
public enum AlertRule {

    SPONSOR_MISSING_CONTACT_PERSON(
            "Missing contact person",
            "A sponsor (not cancelled) has no contact person recorded.",
            Severity.LOW),
    SPONSOR_NO_CONTACT_DETAILS(
            "No email or phone",
            "A sponsor (not cancelled) has neither an email address nor a phone number.",
            Severity.MEDIUM),
    SPONSOR_NOT_LINKED_TO_EVENT(
            "Not linked to an event",
            "A sponsor (not cancelled) is not linked to any event.",
            Severity.LOW),
    SPONSOR_COMMITTED_WITHOUT_AMOUNT(
            "Committed sponsorship without amount",
            "A Confirmed or Completed sponsorship has no sponsorship amount.",
            Severity.HIGH),
    SPONSOR_UNDECIDED_FOR_PAST_EVENT(
            "Undecided sponsorship for a past event",
            "A Proposed or Pending sponsorship is linked to an event whose date has already passed.",
            Severity.HIGH),
    SPONSOR_PAYMENT_OVERDUE(
            "Payment overdue",
            "A sponsorship still has an unpaid balance after its payment due date.",
            Severity.HIGH),
    EVENT_WITHOUT_ACTIVE_SPONSORS(
            "Upcoming event without sponsors",
            "An upcoming event has no sponsors, or only cancelled ones.",
            Severity.MEDIUM),
    EVENT_BELOW_TARGET(
            "Event below sponsorship target",
            "An event in the next " + AlertRule.TARGET_WINDOW_DAYS
                    + " days has less committed sponsorship than its target.",
            Severity.MEDIUM);

    /** How far ahead the "below target" rule looks. */
    public static final int TARGET_WINDOW_DAYS = 30;

    public enum Severity {
        HIGH("High"), MEDIUM("Medium"), LOW("Low");

        private final String label;

        Severity(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String title;
    private final String description;
    private final Severity severity;

    AlertRule(String title, String description, Severity severity) {
        this.title = title;
        this.description = description;
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }
}
