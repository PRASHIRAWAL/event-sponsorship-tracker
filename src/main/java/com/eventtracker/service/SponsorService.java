package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.SponsorRepository;
import com.eventtracker.repository.SponsorSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SponsorService {

    private static final Sort DEFAULT_SORT = Sort.by("name", "id");

    private final SponsorRepository sponsorRepository;
    private final EventRepository eventRepository;

    public SponsorService(SponsorRepository sponsorRepository, EventRepository eventRepository) {
        this.sponsorRepository = sponsorRepository;
        this.eventRepository = eventRepository;
    }

    public List<Sponsor> findAll() {
        return search(null, null, null);
    }

    /** Searches by keyword (name, contact person or email), status and event; null filters are ignored. */
    public List<Sponsor> search(String keyword, SponsorshipStatus status, Long eventId) {
        Specification<Sponsor> specification = Specification
                .where(SponsorSpecifications.matchesKeyword(keyword))
                .and(SponsorSpecifications.hasStatus(status))
                .and(SponsorSpecifications.forEvent(eventId));
        return sponsorRepository.findAll(specification, DEFAULT_SORT);
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
        return sponsorRepository.save(sponsor);
    }

    @Transactional
    public Sponsor update(Long id, SponsorForm form) {
        Sponsor existing = findById(id);
        applyForm(form, existing);
        return sponsorRepository.save(existing);
    }

    private void applyForm(SponsorForm form, Sponsor sponsor) {
        sponsor.setName(form.getName().trim());
        sponsor.setContactPerson(blankToNull(form.getContactPerson()));
        sponsor.setEmail(blankToNull(form.getEmail()));
        sponsor.setPhone(blankToNull(form.getPhone()));
        sponsor.setSponsorshipAmount(form.getSponsorshipAmount());
        sponsor.setSponsorshipStatus(form.getSponsorshipStatus());
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
