package com.eventtracker.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a sponsor refers to an event id that does not exist. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEventReferenceException extends RuntimeException {

    public static final String MESSAGE = "Selected event does not exist";

    private final Long eventId;

    public InvalidEventReferenceException(Long eventId) {
        super(MESSAGE + " (id " + eventId + ")");
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }
}
