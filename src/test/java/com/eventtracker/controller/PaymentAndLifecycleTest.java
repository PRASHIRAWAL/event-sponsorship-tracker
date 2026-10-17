package com.eventtracker.controller;

import com.eventtracker.model.ActivityType;
import com.eventtracker.model.Alert;
import com.eventtracker.model.Event;
import com.eventtracker.model.PaymentForm;
import com.eventtracker.model.PaymentMethod;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import com.eventtracker.repository.ActivityLogRepository;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.PaymentRepository;
import com.eventtracker.repository.SponsorRepository;
import com.eventtracker.service.PaymentService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Integration tests for payments, deletes, paging, exports, reports and the activity log. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentAndLifecycleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private PaymentService paymentService;

    private Event event;
    private Sponsor acme;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        event = new Event("Tech Fest", LocalDate.now().plusDays(10), "Main Auditorium", null);
        event.setSponsorshipTarget(new BigDecimal("50000"));
        event = eventRepository.save(event);
        acme = new Sponsor("Acme Corp", SponsorshipStatus.CONFIRMED);
        acme.setContactPerson("Jane Doe");
        acme.setEmail("jane@acme.example");
        acme.setSponsorshipAmount(new BigDecimal("10000"));
        acme.setTier(SponsorshipTier.GOLD);
        acme.setEvent(event);
        acme = sponsorRepository.save(acme);
    }

    @AfterEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        sponsorRepository.deleteAll();
        eventRepository.deleteAll();
        activityLogRepository.deleteAll();
    }

    @Test
    void recordingAPaymentUpdatesTotalsStatusAndActivity() throws Exception {
        mockMvc.perform(post("/sponsors/{id}/payments", acme.getId())
                        .param("amount", "4000")
                        .param("paymentDate", LocalDate.now().toString())
                        .param("method", "UPI")
                        .param("reference", "UTR-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sponsors/" + acme.getId()))
                .andExpect(flash().attribute("successMessage", "Payment of 4,000.00 recorded for Acme Corp."));

        Sponsor reloaded = sponsorRepository.findById(acme.getId()).orElseThrow();
        assertThat(reloaded.getAmountPaid()).isEqualByComparingTo("4000");
        assertThat(reloaded.getPaymentStatus().name()).isEqualTo("PARTIAL");

        mockMvc.perform(get("/sponsors/{id}", acme.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsors/detail"))
                .andExpect(model().attribute("payments", hasSize(1)))
                .andExpect(content().string(containsString("UTR-1")))
                .andExpect(content().string(containsString("6,000.00")))
                .andExpect(content().string(containsString("Partially paid")));

        assertThat(activityLogRepository.findAll())
                .anyMatch(entry -> entry.getType() == ActivityType.PAYMENT_RECORDED
                        && entry.getSubjectId().equals(acme.getId()));
    }

    @Test
    void paymentLargerThanOutstandingBalanceIsRejectedOnTheForm() throws Exception {
        mockMvc.perform(post("/sponsors/{id}/payments", acme.getId())
                        .param("amount", "10000.01")
                        .param("paymentDate", LocalDate.now().toString())
                        .param("method", "CASH"))
                .andExpect(status().isOk())
                .andExpect(view().name("sponsors/detail"))
                .andExpect(model().attributeHasFieldErrors("payment", "amount"))
                .andExpect(content().string(containsString("Payment exceeds the outstanding balance of 10,000.00")));

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void futurePaymentDateIsRejected() throws Exception {
        mockMvc.perform(post("/sponsors/{id}/payments", acme.getId())
                        .param("amount", "100")
                        .param("paymentDate", LocalDate.now().plusDays(1).toString())
                        .param("method", "CASH"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("payment", "paymentDate"))
                .andExpect(content().string(containsString("Payment date cannot be in the future")));
    }

    @Test
    void paymentsApiRecordsListsAndRejectsCancelledSponsorships() throws Exception {
        mockMvc.perform(post("/api/sponsors/{id}/payments", acme.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 10000, \"paymentDate\": \"" + LocalDate.now() + "\", \"method\": \"CARD\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/payments/")))
                .andExpect(jsonPath("$.amount").value(10000));

        mockMvc.perform(get("/api/sponsors/{id}", acme.getId()))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.outstandingAmount").value(0))
                .andExpect(jsonPath("$.tier").value("GOLD"));
        mockMvc.perform(get("/api/sponsors/{id}/payments", acme.getId()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].method").value("CARD"));

        Sponsor cancelled = new Sponsor("Globex", SponsorshipStatus.CANCELLED);
        cancelled.setSponsorshipAmount(new BigDecimal("500"));
        cancelled = sponsorRepository.save(cancelled);
        mockMvc.perform(post("/api/sponsors/{id}/payments", cancelled.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1, \"paymentDate\": \"" + LocalDate.now() + "\", \"method\": \"CASH\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.amount").value("Payments cannot be recorded for a cancelled sponsorship"));
    }

    @Test
    void deletingAPaymentRecalculatesTheAmountPaid() throws Exception {
        Long first = paymentService.record(acme.getId(), payment("3000")).getId();
        paymentService.record(acme.getId(), payment("2000"));

        mockMvc.perform(post("/sponsors/{id}/payments/{pid}/delete", acme.getId(), first))
                .andExpect(redirectedUrl("/sponsors/" + acme.getId()));

        assertThat(sponsorRepository.findById(acme.getId()).orElseThrow().getAmountPaid()).isEqualByComparingTo("2000");
        mockMvc.perform(delete("/api/payments/{id}", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void sponsorAmountCannotDropBelowWhatWasPaid() throws Exception {
        paymentService.record(acme.getId(), payment("6000"));

        mockMvc.perform(post("/sponsors/{id}", acme.getId())
                        .param("name", "Acme Corp")
                        .param("sponsorshipStatus", "CONFIRMED")
                        .param("sponsorshipAmount", "5000"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("sponsor", "sponsorshipAmount"))
                .andExpect(content().string(containsString("cannot be less than the 6,000.00 already paid")));

        mockMvc.perform(put("/api/sponsors/{id}", acme.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Acme Corp\", \"sponsorshipStatus\": \"CONFIRMED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sponsorshipAmount").exists());
    }

    @Test
    void deletingASponsorRemovesItsPayments() throws Exception {
        paymentService.record(acme.getId(), payment("1000"));

        mockMvc.perform(post("/sponsors/{id}/delete", acme.getId()))
                .andExpect(redirectedUrl("/sponsors"))
                .andExpect(flash().attribute("successMessage", "Sponsor \"Acme Corp\" deleted."));

        assertThat(sponsorRepository.findById(acme.getId())).isEmpty();
        assertThat(paymentRepository.count()).isZero();
        mockMvc.perform(get("/sponsors/{id}", acme.getId())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/sponsors/{id}", acme.getId())).andExpect(status().isNotFound());
    }

    @Test
    void deletingAnEventKeepsItsSponsorsUnlinked() throws Exception {
        mockMvc.perform(post("/events/{id}/delete", event.getId()))
                .andExpect(redirectedUrl("/events"))
                .andExpect(flash().attribute("successMessage", containsString("1 sponsor(s) were kept")));

        assertThat(eventRepository.findById(event.getId())).isEmpty();
        assertThat(sponsorRepository.findById(acme.getId()).orElseThrow().getEvent()).isNull();
        assertThat(activityLogRepository.findAll())
                .anyMatch(entry -> entry.getType() == ActivityType.DELETED && Alert.EVENT.equals(entry.getSubjectType()));
    }

    @Test
    void eventApiDeleteReturnsNoContentAndFundingIsReported() throws Exception {
        mockMvc.perform(get("/api/events/{id}/funding", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.committed").value(10000))
                .andExpect(jsonPath("$.target").value(50000))
                .andExpect(jsonPath("$.percentOfTarget").value(20));

        mockMvc.perform(delete("/api/events/{id}", event.getId())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/events/{id}", event.getId())).andExpect(status().isNotFound());
    }

    @Test
    void sponsorListFiltersByTierAndPaymentStatusAndPaginates() throws Exception {
        Sponsor overdue = new Sponsor("Initech", SponsorshipStatus.CONFIRMED);
        overdue.setSponsorshipAmount(new BigDecimal("2000"));
        overdue.setPaymentDueDate(LocalDate.now().minusDays(3));
        sponsorRepository.save(overdue);

        mockMvc.perform(get("/sponsors").param("tier", "GOLD"))
                .andExpect(model().attribute("sponsors", hasSize(1)))
                .andExpect(content().string(containsString("Acme Corp")))
                .andExpect(content().string(not(containsString("Initech"))));
        mockMvc.perform(get("/sponsors").param("payment", "OVERDUE"))
                .andExpect(model().attribute("sponsors", hasSize(1)))
                .andExpect(content().string(containsString("Initech")));
        mockMvc.perform(get("/api/sponsors").param("paymentStatus", "UNPAID"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Acme Corp"));

        for (int i = 0; i < 11; i++) {
            sponsorRepository.save(new Sponsor("Bulk " + i, SponsorshipStatus.PROPOSED));
        }
        mockMvc.perform(get("/sponsors").param("page", "1").param("sort", "name"))
                .andExpect(model().attribute("sponsors", hasSize(3)))
                .andExpect(content().string(containsString("Showing 11–13 of 13 sponsors")));
        // A page past the end shows the last page instead of an empty list
        mockMvc.perform(get("/sponsors").param("page", "99"))
                .andExpect(model().attribute("sponsors", hasSize(3)));
    }

    @Test
    void eventListShowsFundingProgressAndSortsByName() throws Exception {
        eventRepository.save(new Event("Alpha Meetup", LocalDate.now().plusDays(40), "Lab", null));

        mockMvc.perform(get("/events").param("sort", "name").param("dir", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("20% of 50,000")))
                .andExpect(content().string(containsString("aria-sort=\"ascending\"")));
    }

    @Test
    void csvExportsUseFiltersAndDownloadHeaders() throws Exception {
        mockMvc.perform(get("/sponsors/export.csv").param("tier", "GOLD"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"sponsors-")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Acme Corp")));

        mockMvc.perform(get("/events/export.csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tech Fest")))
                .andExpect(content().string(containsString("50000.00,10000.00")));
    }

    @Test
    void reportsActivityAndHomePagesRender() throws Exception {
        mockMvc.perform(get("/")).andExpect(redirectedUrl("/dashboard"));
        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports"))
                .andExpect(content().string(containsString("Event funding")))
                .andExpect(content().string(containsString("Tech Fest")));
        mockMvc.perform(get("/activity").param("type", "SPONSOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("activity"));
        mockMvc.perform(get("/alerts").param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("severity", com.eventtracker.model.AlertRule.Severity.HIGH));
        mockMvc.perform(get("/api/reports/tiers")).andExpect(jsonPath("$", hasSize(6)));
        mockMvc.perform(get("/api/reports/payment-methods")).andExpect(jsonPath("$", hasSize(5)));
        mockMvc.perform(get("/api/activity").param("limit", "500")).andExpect(status().isOk());
    }

    private static PaymentForm payment(String amount) {
        PaymentForm form = new PaymentForm();
        form.setAmount(new BigDecimal(amount));
        form.setPaymentDate(LocalDate.now());
        form.setMethod(PaymentMethod.BANK_TRANSFER);
        return form;
    }
}
