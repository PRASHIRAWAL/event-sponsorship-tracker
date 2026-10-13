package com.eventtracker.repository;

import com.eventtracker.model.Event;
import com.eventtracker.model.EventStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/** Reusable query filters for searching events. A null result means "no filter". */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> matchesKeyword(String keyword) {
        if (SearchPatterns.isBlank(keyword)) {
            return null;
        }
        String pattern = SearchPatterns.contains(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, SearchPatterns.ESCAPE),
                cb.like(cb.lower(root.get("venue")), pattern, SearchPatterns.ESCAPE));
    }

    public static Specification<Event> hasStatus(EventStatus status, LocalDate today) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case UPCOMING -> (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), today);
            case PAST -> (root, query, cb) -> cb.lessThan(root.get("eventDate"), today);
        };
    }
}
