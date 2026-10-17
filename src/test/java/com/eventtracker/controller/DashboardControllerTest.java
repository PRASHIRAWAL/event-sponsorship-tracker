package com.eventtracker.controller;

import com.eventtracker.model.AlertRule;
import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.SponsorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static com.eventtracker.controller.SponsorApiControllerTest.sponsor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Integration tests for the dashboard, alerts, event search and event drill-down pages. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private EventRepository eventRepository;

    private Event upcoming;
    private Event past;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        upcoming = eventRepository.save(new Event("Tech Fest", LocalDate.of(2099, 11, 5), "Main Auditorium", null));
        past = eventRepository.save(new Event("Alumni Meet", LocalDate.of(2000, 1, 15), "Seminar Hall", null));
        sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.CONFIRMED, upcoming));
        Sponsor incomplete = new Sponsor("Globex", SponsorshipStatus.PENDING);
        incomplete.setEvent(past);
        sponsorRepository.save(incomplete);
    }

    @AfterEach
    void cleanDatabase() {
        sponsorRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void dashboardShowsSummaryIndicators() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("id=\"total-events\">2<")))
                .andExpect(content().string(containsString("id=\"upcoming-events\">1<")))
                .andExpect(content().string(containsString("id=\"total-sponsors\">2<")))
                .andExpect(content().string(containsString("id=\"committed-amount\">10,000.00<")))
                .andExpect(content().string(containsString("id=\"alert-count\">3<")))
                .andExpect(content().string(containsString("/sponsors?status=CONFIRMED")));
    }

    @Test
    void dashboardSearchFindsEventsAndSponsors() throws Exception {
        mockMvc.perform(get("/dashboard").param("q", "acme"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("eventResults", hasSize(0)))
                .andExpect(model().attribute("sponsorResults", hasSize(1)))
                .andExpect(content().string(containsString("Search results for")));

        mockMvc.perform(get("/dashboard").param("q", "seminar"))
                .andExpect(model().attribute("eventResults", hasSize(1)))
                .andExpect(content().string(containsString("Alumni Meet")));
    }

    @Test
    void alertsPageListsIssuesAndRules() throws Exception {
        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("alerts"))
                .andExpect(model().attribute("alerts", hasSize(3)))
                .andExpect(content().string(containsString("Missing contact person")))
                .andExpect(content().string(containsString("No email or phone")))
                .andExpect(content().string(containsString("Undecided sponsorship for a past event")))
                .andExpect(content().string(containsString("Rules Applied")));
    }

    @Test
    void alertsPageShowsOneSeverityPerAlertWithoutChangingTheAlerts() throws Exception {
        String html = mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Globex: undecided for a past event (High), no email or phone (Medium), missing contact person (Low)
        assertThat(countOccurrences(html, "class=\"card alert-card ")).isEqualTo(3);
        assertThat(countOccurrences(html, "class=\"alert-card-icon\"")).isEqualTo(3);
        assertThat(html).contains("severity severity-high", "severity severity-medium", "severity severity-low");
        // One severity badge per rule card as well (8 rules)
        assertThat(countOccurrences(html, "class=\"card rule-card\"")).isEqualTo(AlertRule.values().length);
        assertThat(AlertRule.values()).hasSize(8);
    }

    @Test
    void dashboardShowsUpcomingEventsAndAttentionPreviews() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("upcomingEvents", hasSize(1)))
                .andExpect(model().attribute("topAlerts", hasSize(3)))
                .andExpect(content().string(containsString("Upcoming Events")))
                .andExpect(content().string(containsString("Needs Attention")))
                .andExpect(content().string(containsString("View all (3)")))
                .andExpect(content().string(containsString("Globex")));
    }

    @Test
    void dashboardPreviewsShowEmptyStatesWithoutData() throws Exception {
        sponsorRepository.deleteAll();
        eventRepository.deleteAll();

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"total-events\">0<")))
                .andExpect(content().string(containsString("No upcoming events")))
                .andExpect(content().string(containsString("All clear")));
    }

    private static int countOccurrences(String text, String fragment) {
        return text.split(java.util.regex.Pattern.quote(fragment), -1).length - 1;
    }

    @Test
    void alertsPageShowsSuccessWhenNothingNeedsAttention() throws Exception {
        sponsorRepository.deleteAll();
        eventRepository.deleteAll();

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No issues found")));
    }

    @Test
    void eventListFiltersByDerivedStatusAndKeyword() throws Exception {
        mockMvc.perform(get("/events").param("status", "PAST"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasSize(1)))
                .andExpect(content().string(containsString("Alumni Meet")))
                .andExpect(content().string(not(containsString("Tech Fest"))));

        mockMvc.perform(get("/events").param("q", "auditorium"))
                .andExpect(model().attribute("events", hasSize(1)))
                .andExpect(content().string(containsString("Tech Fest")));

        mockMvc.perform(get("/events").param("q", "no-such-event"))
                .andExpect(content().string(containsString("No events match your search")));
    }

    @Test
    void eventDetailShowsLinkedSponsorsAndCommittedTotal() throws Exception {
        mockMvc.perform(get("/events/{id}", upcoming.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/detail"))
                .andExpect(model().attribute("sponsors", hasSize(1)))
                .andExpect(content().string(containsString("Acme Corp")))
                .andExpect(content().string(containsString("Upcoming")))
                .andExpect(content().string(containsString("10,000.00")));
    }

    @Test
    void unknownEventDetailReturns404() throws Exception {
        mockMvc.perform(get("/events/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void summaryApiReturnsIndicators() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(2))
                .andExpect(jsonPath("$.upcomingEvents").value(1))
                .andExpect(jsonPath("$.totalSponsors").value(2))
                .andExpect(jsonPath("$.committedAmount").value(10000.00))
                .andExpect(jsonPath("$.sponsorsByStatus", hasSize(5)));
    }

    @Test
    void exceptionsApiReturnsAlerts() throws Exception {
        mockMvc.perform(get("/api/dashboard/exceptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].subjectType").value("SPONSOR"))
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    void eventApiResponseIsUnchangedByDerivedStatus() throws Exception {
        mockMvc.perform(get("/api/events/{id}", upcoming.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tech Fest"))
                .andExpect(jsonPath("$.status").doesNotExist());
    }
}
