package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void findAllReturnsEventsOrderedByDate() {
        Event first = new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", null);
        Event second = new Event("Cultural Night", LocalDate.of(2026, 12, 1), "Open Grounds", null);
        when(eventRepository.findAllByOrderByEventDateAscIdAsc()).thenReturn(List.of(first, second));

        assertThat(eventService.findAll()).containsExactly(first, second);
    }

    @Test
    void createIgnoresClientIdAndTrimsFields() {
        Event input = new Event("  Tech Fest  ", LocalDate.of(2026, 11, 5), "  Main Auditorium ", "   ");
        input.setId(99L);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event saved = eventService.create(input);

        assertThat(saved.getId()).isNull();
        assertThat(saved.getName()).isEqualTo("Tech Fest");
        assertThat(saved.getVenue()).isEqualTo("Main Auditorium");
        assertThat(saved.getDescription()).isNull();
    }

    @Test
    void updateCopiesNewValuesOntoExistingEvent() {
        Event existing = new Event("Tech Fest", LocalDate.of(2026, 11, 5), "Main Auditorium", "Old");
        existing.setId(1L);
        Event changes = new Event("Tech Fest 2026", LocalDate.of(2026, 11, 6), "Seminar Hall", "New");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        Event updated = eventService.update(1L, changes);

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("Tech Fest 2026");
        assertThat(updated.getEventDate()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(updated.getVenue()).isEqualTo("Seminar Hall");
        assertThat(updated.getDescription()).isEqualTo("New");
    }

    @Test
    void updateThrowsWhenEventDoesNotExist() {
        when(eventRepository.findById(42L)).thenReturn(Optional.empty());
        Event changes = new Event("Name", LocalDate.of(2026, 11, 6), "Venue", null);

        assertThatThrownBy(() -> eventService.update(42L, changes))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessage("Event not found with id 42");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenEventDoesNotExist() {
        when(eventRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(7L))
                .isInstanceOf(EventNotFoundException.class);
    }
}
