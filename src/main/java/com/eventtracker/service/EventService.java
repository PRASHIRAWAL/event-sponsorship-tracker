package com.eventtracker.service;

import com.eventtracker.model.ActivityType;
import com.eventtracker.model.Alert;
import com.eventtracker.model.Event;
import com.eventtracker.model.EventStatus;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.repository.EventSpecifications;
import com.eventtracker.repository.SponsorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final SponsorRepository sponsorRepository;
    private final ActivityService activityService;

    public EventService(EventRepository eventRepository, SponsorRepository sponsorRepository,
                        ActivityService activityService) {
        this.eventRepository = eventRepository;
        this.sponsorRepository = sponsorRepository;
        this.activityService = activityService;
    }

    public List<Event> findAll() {
        return eventRepository.findAllByOrderByEventDateAscIdAsc();
    }

    /** Searches by keyword (name or venue) and derived status; null or blank filters are ignored. */
    public List<Event> search(String keyword, EventStatus status) {
        return eventRepository.findAll(specification(keyword, status), Sort.by("eventDate", "id"));
    }

    /** Paged search for the event list; the pageable carries the (already validated) sort order. */
    public Page<Event> searchPage(String keyword, EventStatus status, Pageable pageable) {
        return eventRepository.findAll(specification(keyword, status), pageable);
    }

    private static Specification<Event> specification(String keyword, EventStatus status) {
        return Specification
                .where(EventSpecifications.matchesKeyword(keyword))
                .and(EventSpecifications.hasStatus(status, LocalDate.now()));
    }

    public Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional
    public Event create(Event event) {
        // Ignore any client-supplied id so a create can never overwrite an existing event
        event.setId(null);
        trimFields(event);
        Event saved = eventRepository.save(event);
        activityService.record(ActivityType.CREATED, Alert.EVENT, saved.getId(),
                "Event " + saved.getName() + " created for " + saved.getEventDate());
        return saved;
    }

    @Transactional
    public Event update(Long id, Event changes) {
        Event existing = findById(id);
        existing.setName(changes.getName());
        existing.setEventDate(changes.getEventDate());
        existing.setVenue(changes.getVenue());
        existing.setDescription(changes.getDescription());
        existing.setSponsorshipTarget(changes.getSponsorshipTarget());
        trimFields(existing);
        Event saved = eventRepository.save(existing);
        activityService.record(ActivityType.UPDATED, Alert.EVENT, saved.getId(), "Event " + saved.getName() + " updated");
        return saved;
    }

    /**
     * Deletes the event. Its sponsors are kept and simply unlinked, so no sponsor or payment history is lost.
     *
     * @return the number of sponsors that were unlinked
     */
    @Transactional
    public int delete(Long id) {
        Event event = findById(id);
        int unlinked = sponsorRepository.unlinkFromEvent(id);
        eventRepository.deleteById(id);
        activityService.record(ActivityType.DELETED, Alert.EVENT, id, "Event " + event.getName() + " deleted"
                + (unlinked == 0 ? "" : " (" + unlinked + " sponsor(s) unlinked)"));
        return unlinked;
    }

    public long countSponsors(Long eventId) {
        return sponsorRepository.countByEventId(eventId);
    }

    private void trimFields(Event event) {
        event.setName(event.getName().trim());
        event.setVenue(event.getVenue().trim());
        if (event.getDescription() != null) {
            String description = event.getDescription().trim();
            event.setDescription(description.isEmpty() ? null : description);
        }
    }
}
