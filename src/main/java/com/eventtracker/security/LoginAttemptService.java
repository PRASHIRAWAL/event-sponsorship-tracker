package com.eventtracker.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an account for a while after too many wrong passwords in a row (brute-force protection).
 * State is kept in memory, so it resets when the application restarts.
 * Only configured account names are tracked, so guessing random user names cannot fill up memory.
 */
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final SecurityProperties properties;
    private final Clock clock;
    private Set<String> knownUsers = Set.of();

    public LoginAttemptService(SecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Called by {@link AppUserDetailsService} once the accounts are set up. */
    void setKnownUsers(Set<String> usernames) {
        this.knownUsers = Set.copyOf(usernames);
    }

    public void loginFailed(String username) {
        String key = key(username);
        if (key == null || !knownUsers.contains(key)) {
            return;
        }
        attempts.compute(key, (k, current) -> {
            Instant now = clock.instant();
            // A lock that has run out starts a fresh count
            int failures = current == null || isExpired(current, now) ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= properties.maxFailedAttempts()
                    ? now.plus(properties.lockDuration()) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    public void loginSucceeded(String username) {
        String key = key(username);
        if (key != null) {
            attempts.remove(key);
        }
    }

    public boolean isLocked(String username) {
        String key = key(username);
        Attempts current = key == null ? null : attempts.get(key);
        return current != null && current.lockedUntil() != null && current.lockedUntil().isAfter(clock.instant());
    }

    public int failures(String username) {
        String key = key(username);
        Attempts current = key == null ? null : attempts.get(key);
        return current == null || isExpired(current, clock.instant()) ? 0 : current.failures();
    }

    private static boolean isExpired(Attempts attempts, Instant now) {
        return attempts.lockedUntil() != null && !attempts.lockedUntil().isAfter(now);
    }

    static String key(String username) {
        return username == null || username.isBlank() ? null : username.trim().toLowerCase(Locale.ROOT);
    }
}
