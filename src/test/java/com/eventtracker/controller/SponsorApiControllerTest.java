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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
@WithMockUser(username = "admin", roles = "ADMIN")
class SponsorApiControllerTest {

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
    void createSponsorReturns201WithEventDetails() throws Exception {
        String body = """
                {"name": "Acme Corp", "contactPerson": "Jane Doe", "email": "jane@acme.com",
                 "phone": "+91 98765 43210", "sponsorshipAmount": 50000.00,
                 "sponsorshipStatus": "CONFIRMED", "eventId": %d}
                """.formatted(event.getId());

        mockMvc.perform(post("/api/sponsors").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/sponsors/" + onlySponsorId())))
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.sponsorshipStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.sponsorshipAmount").value(50000.00))
                .andExpect(jsonPath("$.eventId").value(event.getId()))
                .andExpect(jsonPath("$.eventName").value("Tech Fest"));

        assertThat(sponsorRepository.count()).isEqualTo(1);
    }

    @Test
    void createWithInvalidFieldsReturns400WithFieldErrors() throws Exception {
        String body = """
                {"name": "", "email": "not-an-email", "phone": "abc", "sponsorshipAmount": -10}
                """;

        mockMvc.perform(post("/api/sponsors").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("Sponsor name is required"))
                .andExpect(jsonPath("$.errors.email").value("Enter a valid email address"))
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.sponsorshipAmount").value("Sponsorship amount must be greater than zero"))
                .andExpect(jsonPath("$.errors.sponsorshipStatus").value("Sponsorship status is required"));

        assertThat(sponsorRepository.count()).isZero();
    }

    @Test
    void createWithUnknownEventReturns400() throws Exception {
        String body = """
                {"name": "Acme Corp", "sponsorshipStatus": "PROPOSED", "eventId": 999999}
                """;

        mockMvc.perform(post("/api/sponsors").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.eventId").value("Selected event does not exist"));

        assertThat(sponsorRepository.count()).isZero();
    }

    @Test
    void createWithUnknownStatusReturns400() throws Exception {
        String body = """
                {"name": "Acme Corp", "sponsorshipStatus": "MAYBE"}
                """;

        mockMvc.perform(post("/api/sponsors").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void listSponsorsSupportsKeywordStatusAndEventFilters() throws Exception {
        sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.CONFIRMED, event));
        sponsorRepository.save(sponsor("Globex", SponsorshipStatus.PROPOSED, event));
        sponsorRepository.save(sponsor("Initech", SponsorshipStatus.CONFIRMED, null));

        mockMvc.perform(get("/api/sponsors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Acme Corp"));
        mockMvc.perform(get("/api/sponsors").param("status", "CONFIRMED"))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/sponsors").param("q", "glob"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Globex"));
        mockMvc.perform(get("/api/sponsors").param("eventId", event.getId().toString()))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchTreatsWildcardCharactersLiterally() throws Exception {
        sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.CONFIRMED, event));

        mockMvc.perform(get("/api/sponsors").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateSponsorChangesStoredValues() throws Exception {
        Sponsor saved = sponsorRepository.save(sponsor("Acme Corp", SponsorshipStatus.PROPOSED, null));
        String body = """
                {"name": "Acme Corporation", "contactPerson": "John Roe", "sponsorshipAmount": 1500.50,
                 "sponsorshipStatus": "CONFIRMED", "eventId": %d}
                """.formatted(event.getId());

        mockMvc.perform(put("/api/sponsors/{id}", saved.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Corporation"))
                .andExpect(jsonPath("$.eventId").value(event.getId()));

        Sponsor reloaded = sponsorRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getSponsorshipStatus()).isEqualTo(SponsorshipStatus.CONFIRMED);
        assertThat(reloaded.getSponsorshipAmount()).isEqualByComparingTo("1500.50");
        assertThat(reloaded.getEmail()).isNull();
        assertThat(reloaded.getEvent().getId()).isEqualTo(event.getId());
    }

    @Test
    void unknownSponsorReturns404() throws Exception {
        String body = """
                {"name": "Acme Corp", "sponsorshipStatus": "PROPOSED"}
                """;

        mockMvc.perform(get("/api/sponsors/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sponsor not found with id 999999"));
        mockMvc.perform(put("/api/sponsors/{id}", 999999).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    private Long onlySponsorId() {
        return sponsorRepository.findAll().get(0).getId();
    }

    static Sponsor sponsor(String name, SponsorshipStatus status, Event event) {
        Sponsor sponsor = new Sponsor(name, status);
        sponsor.setContactPerson("Contact Person");
        sponsor.setEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        sponsor.setSponsorshipAmount(new BigDecimal("10000.00"));
        sponsor.setEvent(event);
        return sponsor;
    }
}
