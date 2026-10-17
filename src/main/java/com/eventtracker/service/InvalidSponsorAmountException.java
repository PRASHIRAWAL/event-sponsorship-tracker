package com.eventtracker.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a sponsor's amount is changed to less than what has already been paid. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSponsorAmountException extends RuntimeException {

    public InvalidSponsorAmountException(String message) {
        super(message);
    }
}
