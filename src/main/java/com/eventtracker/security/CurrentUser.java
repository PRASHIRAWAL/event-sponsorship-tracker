package com.eventtracker.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Name of the signed-in user, used to record who made a change. */
public final class CurrentUser {

    /** Changes made outside a signed-in request, such as by the demo data loader. */
    public static final String SYSTEM = "system";

    private CurrentUser() {
    }

    public static String name() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM;
        }
        return authentication.getName();
    }
}
