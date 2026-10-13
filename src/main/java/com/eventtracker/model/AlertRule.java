package com.eventtracker.model;

/**
 * The rules used to raise alerts. Each rule is deliberately simple and is shown on the Alerts page
 * so users can see exactly why an item was flagged. Cancelled sponsorships never raise alerts.
 */
public enum AlertRule {

    SPONSOR_MISSING_CONTACT_PERSON(
            "Missing contact person",
            "A sponsor (not cancelled) has no contact person recorded."),
    SPONSOR_NO_CONTACT_DETAILS(
            "No email or phone",
            "A sponsor (not cancelled) has neither an email address nor a phone number."),
    SPONSOR_NOT_LINKED_TO_EVENT(
            "Not linked to an event",
            "A sponsor (not cancelled) is not linked to any event."),
    SPONSOR_COMMITTED_WITHOUT_AMOUNT(
            "Committed sponsorship without amount",
            "A Confirmed or Completed sponsorship has no sponsorship amount."),
    SPONSOR_UNDECIDED_FOR_PAST_EVENT(
            "Undecided sponsorship for a past event",
            "A Proposed or Pending sponsorship is linked to an event whose date has already passed."),
    EVENT_WITHOUT_ACTIVE_SPONSORS(
            "Upcoming event without sponsors",
            "An upcoming event has no sponsors, or only cancelled ones.");

    private final String title;
    private final String description;

    AlertRule(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
