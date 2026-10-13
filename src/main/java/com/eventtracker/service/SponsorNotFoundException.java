package com.eventtracker.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SponsorNotFoundException extends RuntimeException {

    public SponsorNotFoundException(Long id) {
        super("Sponsor not found with id " + id);
    }
}
