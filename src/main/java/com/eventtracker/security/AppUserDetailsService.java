package com.eventtracker.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The application's accounts, configured through {@link SecurityProperties} (there is no user table).
 * Passwords are kept only as BCrypt hashes. Accounts locked by {@link LoginAttemptService}
 * are reported as locked, so Spring Security rejects them before checking the password.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AppUserDetailsService.class);

    private final Map<String, UserDetails> users = new HashMap<>();
    private final LoginAttemptService loginAttempts;

    public AppUserDetailsService(SecurityProperties properties, PasswordEncoder encoder,
                                 LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
        String adminPassword = properties.adminPassword();
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = randomPassword();
            log.warn("ADMIN_PASSWORD is not set. Generated a password for '{}' for this run only: {}",
                    properties.adminUsername(), adminPassword);
        }
        add(properties.adminUsername(), adminPassword, Roles.ADMIN, encoder);
        if (properties.viewerEnabled()) {
            add(properties.viewerUsername(), properties.viewerPassword(), Roles.VIEWER, encoder);
        } else {
            log.info("VIEWER_PASSWORD is not set, so the read-only viewer account is disabled.");
        }
        loginAttempts.setKnownUsers(users.keySet());
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String key = LoginAttemptService.key(username);
        UserDetails user = key == null ? null : users.get(key);
        if (user == null) {
            throw new UsernameNotFoundException("Unknown user");
        }
        // A fresh copy each time: Spring Security erases the password of the object it is given
        return User.withUserDetails(user).accountLocked(loginAttempts.isLocked(username)).build();
    }

    private void add(String username, String password, String role, PasswordEncoder encoder) {
        String key = LoginAttemptService.key(username);
        if (key == null) {
            throw new IllegalStateException("The " + role.toLowerCase(Locale.ROOT) + " user name must not be blank");
        }
        if (users.containsKey(key)) {
            throw new IllegalStateException("VIEWER_USERNAME must be different from ADMIN_USERNAME");
        }
        users.put(key, User.withUsername(username.trim())
                .password(encoder.encode(password))
                .roles(role)
                .build());
    }

    private static String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
