package com.eventtracker.controller;

import com.eventtracker.model.Event;
import com.eventtracker.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventApiController {

    private final EventService eventService;

    public EventApiController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.findAll();
    }

    @GetMapping("/{id}")
    public Event getEvent(@PathVariable Long id) {
        return eventService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event) {
        Event created = eventService.create(event);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable Long id, @Valid @RequestBody Event event) {
        return eventService.update(id, event);
    }
}
