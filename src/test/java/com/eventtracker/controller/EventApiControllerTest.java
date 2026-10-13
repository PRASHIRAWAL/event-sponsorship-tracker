package com.eventtracker.controller;

import com.eventtracker.model.Event;
import com.eventtracker.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void clearDatabase() {
        eventRepository.deleteAll();
    }

    @Test
    void createEventReturns201AndPersistsEvent() throws Exception {
        String body = """
                {"name": "Tech Fest", "eventDate": "2026-11-05", "venue": "Main Auditorium",
                 "description": "Annual technical festival"}
                """;

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/events/" + onlyEventId())))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Tech Fest"))
                .andExpect(jsonPath("$.eventDate").value("2026-11-05"))
                .andExpect(jsonPath("$.venue").value("Main Auditorium"));

        assertThat(eventRepository.count()).isEqualTo(1);
    }

    @Test
    void listEventsReturnsEventsOrderedByDate() throws Exception {
        eventRepository.save(new Event("Cultural Night", LocalDate.of(2026, 12, 1), "Open Grounds", null));
        eventRepository.save(new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Tech Fest"))
                .andExpect(jsonPath("$[1].name").value("Cultural Night"));
    }

    @Test
    void getEventByIdReturnsEvent() throws Exception {
        Event saved = eventRepository.save(new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null));

        mockMvc.perform(get("/api/events/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tech Fest"));
    }

    @Test
    void updateEventChangesStoredValues() throws Exception {
        Event saved = eventRepository.save(new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null));
        String body = """
                {"name": "Tech Fest 2026", "eventDate": "2026-11-06", "venue": "Seminar Hall",
                 "description": "Rescheduled"}
                """;

        mockMvc.perform(put("/api/events/{id}", saved.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Tech Fest 2026"));

        Event reloaded = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Tech Fest 2026");
        assertThat(reloaded.getEventDate()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(reloaded.getVenue()).isEqualTo("Seminar Hall");
        assertThat(reloaded.getDescription()).isEqualTo("Rescheduled");
    }

    @Test
    void createWithMissingFieldsReturns400WithFieldErrors() throws Exception {
        String body = """
                {"name": "  ", "description": "No date or venue"}
                """;

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Event name is required"))
                .andExpect(jsonPath("$.errors.eventDate").value("Event date is required"))
                .andExpect(jsonPath("$.errors.venue").value("Venue is required"));

        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void createWithInvalidDateReturns400() throws Exception {
        String body = """
                {"name": "Tech Fest", "eventDate": "05-11-2026", "venue": "Main Auditorium"}
                """;

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());

        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void updateUnknownEventReturns404() throws Exception {
        String body = """
                {"name": "Tech Fest", "eventDate": "2026-11-05", "venue": "Main Auditorium"}
                """;

        mockMvc.perform(put("/api/events/{id}", 9999).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Event not found with id 9999"));
    }

    @Test
    void getUnknownEventReturns404() throws Exception {
        mockMvc.perform(get("/api/events/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/events/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    private Long onlyEventId() {
        return eventRepository.findAll().get(0).getId();
    }
}
