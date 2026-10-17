package com.eventtracker.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Feeds sign-in results (form login, remember-me and HTTP Basic) into the {@link LoginAttemptService}. */
@Component
public class LoginAttemptListener {

    private final LoginAttemptService loginAttempts;

    public LoginAttemptListener(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttempts.loginFailed(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttempts.loginSucceeded(event.getAuthentication().getName());
    }
}
