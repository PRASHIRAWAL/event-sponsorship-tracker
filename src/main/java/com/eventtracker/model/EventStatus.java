package com.eventtracker.model;

import java.time.LocalDate;

/**
 * Event status is derived from the event date and is not stored in the database:
 * an event dated today or later is UPCOMING, an earlier event is PAST.
 */
public enum EventStatus {

    UPCOMING("Upcoming"),
    PAST("Past");

    private final String label;

    EventStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EventStatus of(LocalDate eventDate, LocalDate today) {
        if (eventDate == null) {
            return null;
        }
        return eventDate.isBefore(today) ? PAST : UPCOMING;
    }
}
