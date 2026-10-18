package com.eventtracker.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Sign-in settings (prefix {@code app.security}), normally supplied through environment variables:
 * ADMIN_USERNAME / ADMIN_PASSWORD, VIEWER_USERNAME / VIEWER_PASSWORD and REMEMBER_ME_KEY.
 *
 * @param adminUsername     account with full access
 * @param adminPassword     blank means a random password is generated and logged once at startup
 * @param viewerUsername    read-only account
 * @param viewerPassword    blank disables the viewer account
 * @param maxFailedAttempts wrong passwords in a row before the account is locked
 * @param lockDuration      how long a locked account stays locked
 * @param rememberMeKey     signs "remember me" cookies; blank means a random key (cookies end on restart)
 * @param demoHint          shows the demo credentials on the sign-in page (dev profile only)
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @DefaultValue("admin") String adminUsername,
        String adminPassword,
        @DefaultValue("viewer") String viewerUsername,
        String viewerPassword,
        @DefaultValue("5") int maxFailedAttempts,
        @DefaultValue("15m") Duration lockDuration,
        String rememberMeKey,
        @DefaultValue("false") boolean demoHint) {

    public boolean viewerEnabled() {
        return viewerPassword != null && !viewerPassword.isBlank();
    }
}
