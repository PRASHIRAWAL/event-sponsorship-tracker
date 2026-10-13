package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> findAll() {
        return eventRepository.findAllByOrderByEventDateAscIdAsc();
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
        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, Event changes) {
        Event existing = findById(id);
        existing.setName(changes.getName());
        existing.setEventDate(changes.getEventDate());
        existing.setVenue(changes.getVenue());
        existing.setDescription(changes.getDescription());
        trimFields(existing);
        return eventRepository.save(existing);
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
