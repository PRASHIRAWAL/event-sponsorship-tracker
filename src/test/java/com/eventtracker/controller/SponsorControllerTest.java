package com.eventtracker.controller;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static com.eventtracker.controller.SponsorApiControllerTest.sponsor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class SponsorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private EventRepository eventRepository;

    private Event event;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        event = eventRepository.save(new Event("Tech Fest", LocalDate.of(2099, 11, 5), "Main Auditorium", null));
    }

    @AfterEach
    void cleanDatabase() {
        sponsorRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void listPageShowsSponsorsWithEventAndStatus() throws Exception {
        sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.CONFIRMED, event));

        mockMvc.perform(get("/sponsors"))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsors/list"))
                .andExpect(model().attribute("sponsors", hasSize(1)))
                .andExpect(content().string(containsString("Acme Corp")))
                .andExpect(content().string(containsString("Tech Fest")))
                .andExpect(content().string(containsString("Confirmed")))
                .andExpect(content().string(containsString("10,000.00")));
    }

    @Test
    void listPageShowsEmptyStateWhenNoSponsors() throws Exception {
        mockMvc.perform(get("/sponsors"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No sponsors yet")));
    }

    @Test
    void listPageFiltersByStatusAndKeyword() throws Exception {
        sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.CONFIRMED, event));
        sponsorRepository.save(sponsor("Globex", SponsorshipStatus.PENDING, event));

        mockMvc.perform(get("/sponsors").param("status", "PENDING"))
                .andExpect(model().attribute("sponsors", hasSize(1)))
                .andExpect(content().string(containsString("Globex")))
                .andExpect(content().string(not(containsString("Acme Corp"))));
        mockMvc.perform(get("/sponsors").param("q", "nothing-matches"))
                .andExpect(content().string(containsString("No sponsors match your search")));
    }

    @Test
    void createFormPreselectsEventFromQueryParameter() throws Exception {
        mockMvc.perform(get("/sponsors/new").param("eventId", event.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsors/form"))
                .andExpect(content().string(containsString("Add Sponsor")))
                .andExpect(content().string(containsString(
                        "value=\"" + event.getId() + "\" selected=\"selected\"")));
    }

    @Test
    void submittingValidFormCreatesSponsorAndRedirects() throws Exception {
        mockMvc.perform(post("/sponsors").with(csrf())
                        .param("name", "Acme Corp")
                        .param("contactPerson", "Jane Doe")
                        .param("email", "jane@acme.com")
                        .param("phone", "")
                        .param("sponsorshipAmount", "25000.00")
                        .param("sponsorshipStatus", "CONFIRMED")
                        .param("eventId", event.getId().toString()))
                .andExpect(redirectedUrl("/sponsors"))
                .andExpect(flash().attribute("successMessage", "Sponsor \"Acme Corp\" created successfully."));

        Sponsor saved = sponsorRepository.findAll().get(0);
        assertThat(saved.getName()).isEqualTo("Acme Corp");
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getSponsorshipAmount()).isEqualByComparingTo("25000.00");
        assertThat(saved.getEvent().getId()).isEqualTo(event.getId());
    }

    @Test
    void submittingInvalidFormShowsValidationErrors() throws Exception {
        mockMvc.perform(post("/sponsors").with(csrf())
                        .param("name", " ")
                        .param("email", "bad-email")
                        .param("sponsorshipAmount", "abc")
                        .param("sponsorshipStatus", "PROPOSED")
                        .param("eventId", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsors/form"))
                .andExpect(model().attributeHasFieldErrors("sponsor", "name", "email", "sponsorshipAmount"))
                .andExpect(content().string(containsString("Sponsor name is required")))
                .andExpect(content().string(containsString("Enter a valid email address")))
                .andExpect(content().string(containsString("Please enter a valid sponsorship amount")));

        assertThat(sponsorRepository.count()).isZero();
    }

    @Test
    void submittingUnknownEventShowsFieldError() throws Exception {
        mockMvc.perform(post("/sponsors").with(csrf())
                        .param("name", "Acme Corp")
                        .param("sponsorshipStatus", "PROPOSED")
                        .param("eventId", "999999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("sponsor", "eventId"))
                .andExpect(content().string(containsString("Selected event does not exist")));

        assertThat(sponsorRepository.count()).isZero();
    }

    @Test
    void editFormIsPrefilledAndUpdateIsSaved() throws Exception {
        Sponsor saved = sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.PROPOSED, event));

        mockMvc.perform(get("/sponsors/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Sponsor")))
                .andExpect(content().string(containsString("value=\"Acme Corp\"")));

        mockMvc.perform(post("/sponsors/{id}", saved.getId()).with(csrf())
                        .param("name", "Acme Corporation")
                        .param("sponsorshipStatus", "COMPLETED")
                        .param("sponsorshipAmount", "30000")
                        .param("eventId", event.getId().toString()))
                .andExpect(redirectedUrl("/sponsors"))
                .andExpect(flash().attribute("successMessage", "Sponsor \"Acme Corporation\" updated successfully."));

        Sponsor reloaded = sponsorRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Acme Corporation");
        assertThat(reloaded.getSponsorshipStatus()).isEqualTo(SponsorshipStatus.COMPLETED);
    }

    @Test
    void invalidUpdateKeepsOriginalSponsor() throws Exception {
        Sponsor saved = sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.PROPOSED, event));

        mockMvc.perform(post("/sponsors/{id}", saved.getId()).with(csrf())
                        .param("name", "")
                        .param("sponsorshipStatus", "PROPOSED"))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsors/form"))
                .andExpect(content().string(containsString("/sponsors/" + saved.getId())));

        assertThat(sponsorRepository.findById(saved.getId()).orElseThrow().getName()).isEqualTo("Acme Corp");
    }

    @Test
    void editingUnknownSponsorReturns404() throws Exception {
        mockMvc.perform(get("/sponsors/{id}/edit", 999999))
                .andExpect(status().isNotFound());
    }
}
