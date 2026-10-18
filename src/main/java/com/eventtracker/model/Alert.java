package com.eventtracker.model;

/**
 * One actionable issue found by an {@link AlertRule}.
 *
 * @param subjectType "EVENT" or "SPONSOR", used to build links to the item that needs attention
 */
public record Alert(AlertRule rule, String subjectType, Long subjectId, String subjectName) {

    public static final String EVENT = "EVENT";
    public static final String SPONSOR = "SPONSOR";

    public static Alert forEvent(AlertRule rule, Event event) {
        return new Alert(rule, EVENT, event.getId(), event.getName());
    }

    public static Alert forSponsor(AlertRule rule, Sponsor sponsor) {
        return new Alert(rule, SPONSOR, sponsor.getId(), sponsor.getName());
    }

    public String getTitle() {
        return rule.getTitle();
    }

    public AlertRule.Severity getSeverity() {
        return rule.getSeverity();
    }
}
