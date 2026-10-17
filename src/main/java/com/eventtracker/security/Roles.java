package com.eventtracker.security;

/** The two roles of the application. ADMIN can change data; VIEWER can only read it. */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String VIEWER = "VIEWER";

    private Roles() {
    }
}
