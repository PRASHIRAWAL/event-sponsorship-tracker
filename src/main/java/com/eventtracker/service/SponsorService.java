package com.eventtracker.service;

import com.eventtracker.model.ActivityType;
import com.eventtracker.model.Alert;
import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorFilter;
import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.PaymentRepository;
import com.eventtracker.repository.SponsorRepository;
import com.eventtracker.repository.SponsorSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SponsorService {

    private static final Sort DEFAULT_SORT = Sort.by("name", "id");

    private final SponsorRepository sponsorRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final ActivityService activityService;

    public SponsorService(SponsorRepository sponsorRepository, EventRepository eventRepository,
                          PaymentRepository paymentRepository, ActivityService activityService) {
        this.sponsorRepository = sponsorRepository;
        this.eventRepository = eventRepository;
        this.paymentRepository = paymentRepository;
        this.activityService = activityService;
    }

    public List<Sponsor> findAll() {
        return search(SponsorFilter.none());
    }

    /** Searches by keyword (name, contact person, email or industry), status and event; null filters are ignored. */
    public List<Sponsor> search(String keyword, SponsorshipStatus status, Long eventId) {
        return search(new SponsorFilter(keyword, status, eventId, null, null));
    }

    public List<Sponsor> search(SponsorFilter filter) {
        return sponsorRepository.findAll(SponsorSpecifications.matching(filter, LocalDate.now()), DEFAULT_SORT);
    }

    /** Paged search for the sponsor list; the pageable carries the (already validated) sort order. */
    public Page<Sponsor> searchPage(SponsorFilter filter, Pageable pageable) {
        return sponsorRepository.findAll(SponsorSpecifications.matching(filter, LocalDate.now()), pageable);
    }

    public List<Sponsor> findByEvent(Long eventId) {
        return search(null, null, eventId);
    }

    public Sponsor findById(Long id) {
        return sponsorRepository.findById(id)
                .orElseThrow(() -> new SponsorNotFoundException(id));
    }

    @Transactional
    public Sponsor create(SponsorForm form) {
        Sponsor sponsor = new Sponsor();
        applyForm(form, sponsor);
        Sponsor saved = sponsorRepository.save(sponsor);
        activityService.record(ActivityType.CREATED, Alert.SPONSOR, saved.getId(),
                "Sponsor " + saved.getName() + " added as " + saved.getSponsorshipStatus().getLabel()
                        + (saved.getEvent() == null ? "" : " for " + saved.getEvent().getName()));
        return saved;
    }

    @Transactional
    public Sponsor update(Long id, SponsorForm form) {
        Sponsor existing = findById(id);
        SponsorshipStatus previousStatus = existing.getSponsorshipStatus();
        checkAmountCoversPayments(existing, form.getSponsorshipAmount());
        applyForm(form, existing);
        Sponsor saved = sponsorRepository.save(existing);
        String change = previousStatus == saved.getSponsorshipStatus() ? ""
                : " (status " + previousStatus.getLabel() + " → " + saved.getSponsorshipStatus().getLabel() + ")";
        activityService.record(ActivityType.UPDATED, Alert.SPONSOR, saved.getId(),
                "Sponsor " + saved.getName() + " updated" + change);
        return saved;
    }

    /** Deletes the sponsor together with its payment history. */
    @Transactional
    public void delete(Long id) {
        Sponsor sponsor = findById(id);
        paymentRepository.deleteBySponsorId(id);
        sponsorRepository.delete(sponsor);
        activityService.record(ActivityType.DELETED, Alert.SPONSOR, id, "Sponsor " + sponsor.getName() + " deleted");
    }

    /** The amount may not drop below what the sponsor has already paid, or be removed once payments exist. */
    private static void checkAmountCoversPayments(Sponsor sponsor, BigDecimal newAmount) {
        BigDecimal paid = sponsor.getAmountPaid();
        if (paid.signum() == 0) {
            return;
        }
        if (newAmount == null || newAmount.compareTo(paid) < 0) {
            throw new InvalidSponsorAmountException(
                    "Sponsorship amount cannot be less than the " + Money.format(paid) + " already paid");
        }
    }

    private void applyForm(SponsorForm form, Sponsor sponsor) {
        sponsor.setName(form.getName().trim());
        sponsor.setContactPerson(blankToNull(form.getContactPerson()));
        sponsor.setEmail(blankToNull(form.getEmail()));
        sponsor.setPhone(blankToNull(form.getPhone()));
        sponsor.setIndustry(blankToNull(form.getIndustry()));
        sponsor.setSponsorshipAmount(form.getSponsorshipAmount());
        sponsor.setSponsorshipStatus(form.getSponsorshipStatus());
        sponsor.setTier(form.getTier());
        sponsor.setPaymentDueDate(form.getPaymentDueDate());
        sponsor.setNotes(blankToNull(form.getNotes()));
        sponsor.setEvent(resolveEvent(form.getEventId()));
    }

    private Event resolveEvent(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new InvalidEventReferenceException(eventId));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
