package com.eventtracker.security;

import com.eventtracker.model.ActivityLog;
import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.repository.ActivityLogRepository;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.PaymentRepository;
import com.eventtracker.repository.SponsorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sign-in, roles and access rules, using the real accounts from the "test" profile. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "admin-test-pass";
    private static final String VIEWER = "viewer";
    private static final String VIEWER_PASSWORD = "viewer-test-pass";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private LoginAttemptService loginAttempts;

    @BeforeEach
    @AfterEach
    void reset() {
        paymentRepository.deleteAll();
        sponsorRepository.deleteAll();
        eventRepository.deleteAll();
        activityLogRepository.deleteAll();
        // Lockouts live in a shared bean, so clear them for the next test
        loginAttempts.loginSucceeded(ADMIN);
        loginAttempts.loginSucceeded(VIEWER);
    }

    @Test
    void pagesRedirectAnonymousVisitorsToSignIn() throws Exception {
        for (String path : List.of("/dashboard", "/events", "/sponsors", "/reports", "/alerts", "/activity",
                "/events/export.csv")) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    void signInPageStaticFilesAndHealthArePublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"login-form\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                // Demo credentials are only shown in the dev profile
                .andExpect(content().string(not(containsString("id=\"demo-accounts\""))));
        mockMvc.perform(get("/css/style.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                // Database details are only shown to signed-in users
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void apiReturns401WithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/events").with(httpBasic(ADMIN, "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanSignInWithTheForm() throws Exception {
        mockMvc.perform(formLogin().user(ADMIN).password(ADMIN_PASSWORD))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername(ADMIN).withRoles(Roles.ADMIN));
    }

    @Test
    void userNameIsNotCaseSensitive() throws Exception {
        mockMvc.perform(formLogin().user("Admin").password(ADMIN_PASSWORD))
                .andExpect(authenticated().withUsername(ADMIN));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin().user(ADMIN).password("not-the-password"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mockMvc.perform(formLogin().user("nobody").password("whatever"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void accountIsLockedAfterFiveWrongPasswords() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(formLogin().user(VIEWER).password("wrong-" + i))
                    .andExpect(redirectedUrl("/login?error"));
        }

        // Even the right password is refused while the account is locked
        mockMvc.perform(formLogin().user(VIEWER).password(VIEWER_PASSWORD))
                .andExpect(redirectedUrl("/login?locked"))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/api/events").with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(status().isUnauthorized());
        // Other accounts are not affected
        mockMvc.perform(formLogin().user(ADMIN).password(ADMIN_PASSWORD)).andExpect(authenticated());
    }

    @Test
    void successfulSignInResetsTheFailureCount() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(formLogin().user(VIEWER).password("wrong-" + i));
        }
        mockMvc.perform(formLogin().user(VIEWER).password(VIEWER_PASSWORD)).andExpect(authenticated());
        assertThat(loginAttempts.failures(VIEWER)).isZero();

        mockMvc.perform(formLogin().user(VIEWER).password("wrong-again")).andExpect(unauthenticated());
        mockMvc.perform(formLogin().user(VIEWER).password(VIEWER_PASSWORD)).andExpect(authenticated());
    }

    @Test
    void signOutEndsTheSession() throws Exception {
        mockMvc.perform(logout()).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void viewerCanReadButNotChangeAnything() throws Exception {
        Event event = eventRepository.save(new Event("Tech Fest", LocalDate.of(2099, 11, 5), "Main Hall", null));

        for (String path : List.of("/dashboard", "/events", "/events/" + event.getId(), "/sponsors", "/reports",
                "/events/export.csv", "/sponsors/export.csv", "/api/events", "/api/reports/events")) {
            mockMvc.perform(get(path).with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/events").with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(content().string(containsString("id=\"current-role\">Viewer<")))
                .andExpect(content().string(not(containsString("id=\"create-event\""))))
                .andExpect(content().string(not(containsString("id=\"quick-add\""))));
        mockMvc.perform(get("/events/" + event.getId()).with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(content().string(not(containsString("id=\"edit-event\""))))
                .andExpect(content().string(not(containsString("id=\"delete-event\""))));

        mockMvc.perform(get("/events/new").with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/events/{id}/edit", event.getId()).with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/events/{id}/delete", event.getId()).with(csrf()).with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/events/{id}", event.getId()).with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(status().isForbidden());

        assertThat(eventRepository.existsById(event.getId())).isTrue();
    }

    @Test
    void viewerCannotSeeAlertsActivityOrPaymentRecords() throws Exception {
        Event event = eventRepository.save(new Event("Tech Fest", LocalDate.of(2099, 11, 5), "Main Hall", null));
        Sponsor sponsor = new Sponsor("Acme Corp", SponsorshipStatus.CONFIRMED);
        sponsor.setEvent(event);
        sponsor.setSponsorshipAmount(new BigDecimal("10000"));
        sponsor = sponsorRepository.save(sponsor);

        for (String path : List.of("/alerts", "/activity", "/api/activity", "/api/dashboard/exceptions",
                "/api/sponsors/" + sponsor.getId() + "/payments")) {
            mockMvc.perform(get(path).with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(path).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/dashboard").with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(content().string(not(containsString("href=\"/alerts\""))))
                .andExpect(content().string(not(containsString("href=\"/activity\""))))
                .andExpect(content().string(not(containsString("id=\"attention-panel\""))))
                .andExpect(content().string(not(containsString("id=\"activity-panel\""))))
                .andExpect(content().string(containsString("id=\"target-card\"")));
        mockMvc.perform(get("/sponsors/{id}", sponsor.getId()).with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"payments-restricted\"")))
                .andExpect(content().string(not(containsString("id=\"record-payment\""))));
        mockMvc.perform(get("/reports").with(httpBasic(VIEWER, VIEWER_PASSWORD)))
                .andExpect(content().string(not(containsString("id=\"payment-methods\""))));

        mockMvc.perform(get("/dashboard").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(content().string(containsString("id=\"attention-panel\"")))
                .andExpect(content().string(not(containsString("id=\"target-card\""))));
    }

    @Test
    void forbiddenPageExplainsReadOnlyAccess() throws Exception {
        mockMvc.perform(get("/sponsors/new").with(httpBasic(VIEWER, VIEWER_PASSWORD)).accept(MediaType.TEXT_HTML))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSeesTheEditControls() throws Exception {
        Event event = eventRepository.save(new Event("Tech Fest", LocalDate.of(2099, 11, 5), "Main Hall", null));

        mockMvc.perform(get("/events/" + event.getId()).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"current-user\">admin<")))
                .andExpect(content().string(containsString("id=\"current-role\">Admin<")))
                .andExpect(content().string(containsString("id=\"edit-event\"")))
                .andExpect(content().string(containsString("id=\"quick-add\"")));
    }

    @Test
    void formPostsNeedACsrfToken() throws Exception {
        mockMvc.perform(post("/events").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("name", "Tech Fest").param("eventDate", "2099-11-05").param("venue", "Main Hall"))
                .andExpect(status().isForbidden());
        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void apiAcceptsHttpBasicWithoutCsrfToken() throws Exception {
        String body = """
                {"name": "Tech Fest", "eventDate": "2099-11-05", "venue": "Main Hall"}
                """;
        mockMvc.perform(post("/api/events").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/events").with(httpBasic(VIEWER, VIEWER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @Test
    void activityLogRecordsWhoMadeTheChange() throws Exception {
        mockMvc.perform(post("/events").with(csrf()).with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .param("name", "Tech Fest").param("eventDate", "2099-11-05").param("venue", "Main Hall"))
                .andExpect(status().is3xxRedirection());

        List<ActivityLog> entries = activityLogRepository.findAll();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getActor()).isEqualTo(ADMIN);

        mockMvc.perform(get("/activity").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(content().string(containsString("class=\"timeline-actor\">admin<")));
    }
}
