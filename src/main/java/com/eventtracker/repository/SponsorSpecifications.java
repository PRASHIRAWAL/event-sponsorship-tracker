package com.eventtracker.repository;

import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import org.springframework.data.jpa.domain.Specification;

/** Reusable query filters for searching sponsors. A null result means "no filter". */
public final class SponsorSpecifications {

    private SponsorSpecifications() {
    }

    public static Specification<Sponsor> matchesKeyword(String keyword) {
        if (SearchPatterns.isBlank(keyword)) {
            return null;
        }
        String pattern = SearchPatterns.contains(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, SearchPatterns.ESCAPE),
                cb.like(cb.lower(root.get("contactPerson")), pattern, SearchPatterns.ESCAPE),
                cb.like(cb.lower(root.get("email")), pattern, SearchPatterns.ESCAPE));
    }

    public static Specification<Sponsor> hasStatus(SponsorshipStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("sponsorshipStatus"), status);
    }

    public static Specification<Sponsor> forEvent(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("event").get("id"), eventId);
    }
}
