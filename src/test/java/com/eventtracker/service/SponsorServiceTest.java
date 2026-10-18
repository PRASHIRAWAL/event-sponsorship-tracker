package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.PaymentRepository;
import com.eventtracker.repository.SponsorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SponsorServiceTest {

    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private SponsorService sponsorService;

    @Test
    void createTrimsTextAndConvertsBlankOptionalFieldsToNull() {
        SponsorForm form = form("  Acme Corp ", SponsorshipStatus.PROPOSED);
        form.setContactPerson("   ");
        form.setEmail("");
        form.setPhone(" 98765 43210 ");
        when(sponsorRepository.save(any(Sponsor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sponsor saved = sponsorService.create(form);

        assertThat(saved.getName()).isEqualTo("Acme Corp");
        assertThat(saved.getContactPerson()).isNull();
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getPhone()).isEqualTo("98765 43210");
        assertThat(saved.getEvent()).isNull();
    }

    @Test
    void createLinksExistingEvent() {
        Event event = new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null);
        event.setId(3L);
        SponsorForm form = form("Acme Corp", SponsorshipStatus.CONFIRMED);
        form.setEventId(3L);
        form.setSponsorshipAmount(new BigDecimal("25000.00"));
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(sponsorRepository.save(any(Sponsor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sponsor saved = sponsorService.create(form);

        assertThat(saved.getEvent()).isSameAs(event);
        assertThat(saved.getSponsorshipAmount()).isEqualByComparingTo("25000.00");
        assertThat(saved.getSponsorshipStatus()).isEqualTo(SponsorshipStatus.CONFIRMED);
    }

    @Test
    void createRejectsUnknownEvent() {
        SponsorForm form = form("Acme Corp", SponsorshipStatus.PROPOSED);
        form.setEventId(99L);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sponsorService.create(form))
                .isInstanceOf(InvalidEventReferenceException.class);
        verify(sponsorRepository, never()).save(any());
    }

    @Test
    void updateChangesExistingSponsor() {
        Sponsor existing = new Sponsor("Acme Corp", SponsorshipStatus.PROPOSED);
        existing.setId(1L);
        SponsorForm form = form("Acme Corporation", SponsorshipStatus.CONFIRMED);
        form.setSponsorshipAmount(new BigDecimal("1000"));
        when(sponsorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sponsorRepository.save(existing)).thenReturn(existing);

        Sponsor updated = sponsorService.update(1L, form);

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("Acme Corporation");
        assertThat(updated.getSponsorshipStatus()).isEqualTo(SponsorshipStatus.CONFIRMED);
    }

    @Test
    void updateThrowsWhenSponsorDoesNotExist() {
        when(sponsorRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sponsorService.update(42L, form("Acme", SponsorshipStatus.PROPOSED)))
                .isInstanceOf(SponsorNotFoundException.class)
                .hasMessage("Sponsor not found with id 42");
    }

    private static SponsorForm form(String name, SponsorshipStatus status) {
        SponsorForm form = new SponsorForm();
        form.setName(name);
        form.setSponsorshipStatus(status);
        return form;
    }
}
