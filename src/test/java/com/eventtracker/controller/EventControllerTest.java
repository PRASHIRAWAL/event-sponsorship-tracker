package com.eventtracker.controller;

import com.eventtracker.model.Event;
import com.eventtracker.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void clearDatabase() {
        eventRepository.deleteAll();
    }

    @Test
    void listPageShowsSavedEvents() throws Exception {
        eventRepository.save(new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", "Annual fest"));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/list"))
                .andExpect(model().attribute("events", hasSize(1)))
                .andExpect(content().string(containsString("Tech Fest")))
                .andExpect(content().string(containsString("05 Nov 2026")));
    }

    @Test
    void listPageShowsEmptyStateWhenNoEvents() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No events yet")));
    }

    @Test
    void createFormIsDisplayed() throws Exception {
        mockMvc.perform(get("/events/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/form"))
                .andExpect(content().string(containsString("Create Event")));
    }

    @Test
    void submittingValidFormCreatesEventAndRedirects() throws Exception {
        mockMvc.perform(post("/events").with(csrf())
                        .param("name", "Tech Fest")
                        .param("eventDate", "2026-11-05")
                        .param("venue", "Main Auditorium")
                        .param("description", "Annual technical festival"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events"))
                .andExpect(flash().attribute("successMessage", "Event \"Tech Fest\" created successfully."));

        assertThat(eventRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getName()).isEqualTo("Tech Fest");
                    assertThat(event.getEventDate()).isEqualTo(LocalDate.of(2026, 11, 5));
                });
    }

    @Test
    void submittingEmptyFormShowsValidationErrors() throws Exception {
        mockMvc.perform(post("/events").with(csrf())
                        .param("name", "   ")
                        .param("eventDate", "")
                        .param("venue", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("events/form"))
                .andExpect(model().attributeHasFieldErrors("event", "name", "eventDate", "venue"))
                .andExpect(content().string(containsString("Event name is required")))
                .andExpect(content().string(containsString("Event date is required")))
                .andExpect(content().string(containsString("Venue is required")));

        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void submittingInvalidDateShowsFriendlyMessage() throws Exception {
        mockMvc.perform(post("/events").with(csrf())
                        .param("name", "Tech Fest")
                        .param("eventDate", "not-a-date")
                        .param("venue", "Main Auditorium"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("event", "eventDate", "typeMismatch"))
                .andExpect(content().string(containsString("Please enter a valid event date (yyyy-MM-dd)")));

        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void editFormIsPrefilledWithExistingValues() throws Exception {
        Event saved = eventRepository.save(new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null));

        mockMvc.perform(get("/events/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/form"))
                .andExpect(content().string(containsString("Edit Event")))
                .andExpect(content().string(containsString("value=\"Tech Fest\"")))
                .andExpect(content().string(containsString("value=\"2026-11-05\"")));
    }

    @Test
    void submittingValidEditUpdatesEvent() throws Exception {
        Event saved = eventRepository.save(new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null));

        mockMvc.perform(post("/events/{id}", saved.getId()).with(csrf())
                        .param("name", "Tech Fest 2026")
                        .param("eventDate", "2026-11-06")
                        .param("venue", "Seminar Hall")
                        .param("description", "Rescheduled"))
                .andExpect(redirectedUrl("/events"))
                .andExpect(flash().attribute("successMessage", "Event \"Tech Fest 2026\" updated successfully."));

        Event reloaded = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Tech Fest 2026");
        assertThat(reloaded.getVenue()).isEqualTo("Seminar Hall");
    }

    @Test
    void submittingInvalidEditKeepsOriginalEvent() throws Exception {
        Event saved = eventRepository.save(new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null));

        mockMvc.perform(post("/events/{id}", saved.getId()).with(csrf())
                        .param("name", "")
                        .param("eventDate", "2026-11-06")
                        .param("venue", "Seminar Hall"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/form"))
                .andExpect(model().attributeHasFieldErrors("event", "name"))
                .andExpect(content().string(containsString("/events/" + saved.getId())));

        assertThat(eventRepository.findById(saved.getId()).orElseThrow().getName()).isEqualTo("Tech Fest");
    }

    @Test
    void editingUnknownEventReturns404() throws Exception {
        mockMvc.perform(get("/events/{id}/edit", 9999))
                .andExpect(status().isNotFound());
    }
}
