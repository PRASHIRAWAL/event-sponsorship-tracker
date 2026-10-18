package com.eventtracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-18T10:00:00Z"));
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties("admin", "secret", "viewer", "secret",
                3, Duration.ofMinutes(15), null, false);
        service = new LoginAttemptService(properties, clock);
        service.setKnownUsers(Set.of("admin", "viewer"));
    }

    @Test
    void locksAfterTheConfiguredNumberOfFailures() {
        service.loginFailed("admin");
        service.loginFailed("admin");
        assertThat(service.isLocked("admin")).isFalse();

        service.loginFailed("admin");

        assertThat(service.isLocked("admin")).isTrue();
        assertThat(service.isLocked("ADMIN")).as("names are not case sensitive").isTrue();
        assertThat(service.isLocked("viewer")).isFalse();
    }

    @Test
    void lockExpiresAfterTheLockDuration() {
        failTimes("admin", 3);

        clock.advance(Duration.ofMinutes(14));
        assertThat(service.isLocked("admin")).isTrue();

        clock.advance(Duration.ofMinutes(1));
        assertThat(service.isLocked("admin")).isFalse();
        assertThat(service.failures("admin")).isZero();

        // After an expired lock the count starts again from one
        service.loginFailed("admin");
        assertThat(service.failures("admin")).isEqualTo(1);
        assertThat(service.isLocked("admin")).isFalse();
    }

    @Test
    void successClearsFailures() {
        failTimes("viewer", 2);

        service.loginSucceeded("viewer");

        assertThat(service.failures("viewer")).isZero();
        failTimes("viewer", 2);
        assertThat(service.isLocked("viewer")).isFalse();
    }

    @Test
    void unknownAndBlankNamesAreIgnored() {
        failTimes("intruder", 10);
        failTimes(" ", 10);
        service.loginFailed(null);

        assertThat(service.isLocked("intruder")).isFalse();
        assertThat(service.failures("intruder")).isZero();
        assertThat(service.isLocked(null)).isFalse();
    }

    private void failTimes(String username, int times) {
        for (int i = 0; i < times; i++) {
            service.loginFailed(username);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
