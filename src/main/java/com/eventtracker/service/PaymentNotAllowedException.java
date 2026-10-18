package com.eventtracker.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a payment breaks a business rule (for example it is larger than the outstanding balance).
 *
 * @see #getField() the form field the message belongs to, so the UI can highlight it
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PaymentNotAllowedException extends RuntimeException {

    private final String field;

    public PaymentNotAllowedException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
