package com.eventtracker.repository;

import com.eventtracker.model.PaymentStatus;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorFilter;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Reusable query filters for searching sponsors. A null result means "no filter". */
public final class SponsorSpecifications {

    private SponsorSpecifications() {
    }

    public static Specification<Sponsor> matching(SponsorFilter filter, LocalDate today) {
        return Specification.where(matchesKeyword(filter.keyword()))
                .and(hasStatus(filter.status()))
                .and(forEvent(filter.eventId()))
                .and(hasTier(filter.tier()))
                .and(hasPaymentStatus(filter.paymentStatus(), today));
    }

    public static Specification<Sponsor> matchesKeyword(String keyword) {
        if (SearchPatterns.isBlank(keyword)) {
            return null;
        }
        String pattern = SearchPatterns.contains(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, SearchPatterns.ESCAPE),
                cb.like(cb.lower(root.get("contactPerson")), pattern, SearchPatterns.ESCAPE),
                cb.like(cb.lower(root.get("email")), pattern, SearchPatterns.ESCAPE),
                cb.like(cb.lower(root.get("industry")), pattern, SearchPatterns.ESCAPE));
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

    public static Specification<Sponsor> hasTier(SponsorshipTier tier) {
        if (tier == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tier"), tier);
    }

    /**
     * Database version of {@link PaymentStatus#of}; both must agree. Only sponsorships that are not
     * cancelled and have an amount have a payment status other than NOT_APPLICABLE.
     */
    public static Specification<Sponsor> hasPaymentStatus(PaymentStatus status, LocalDate today) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> {
            Expression<BigDecimal> amount = root.get("sponsorshipAmount");
            Expression<BigDecimal> paid = cb.coalesce(root.<BigDecimal>get("amountPaid"), BigDecimal.ZERO);
            Predicate applicable = cb.and(
                    cb.notEqual(root.get("sponsorshipStatus"), SponsorshipStatus.CANCELLED),
                    cb.isNotNull(amount));
            Predicate fullyPaid = cb.greaterThanOrEqualTo(paid, amount);
            Predicate pastDue = cb.lessThan(root.<LocalDate>get("paymentDueDate"), today);
            Predicate notPastDue = cb.or(cb.isNull(root.get("paymentDueDate")),
                    cb.greaterThanOrEqualTo(root.<LocalDate>get("paymentDueDate"), today));
            return switch (status) {
                case PAID -> cb.and(applicable, fullyPaid);
                case OVERDUE -> cb.and(applicable, cb.not(fullyPaid), pastDue);
                case PARTIAL -> cb.and(applicable, cb.not(fullyPaid), notPastDue, cb.greaterThan(paid, BigDecimal.ZERO));
                case UNPAID -> cb.and(applicable, cb.not(fullyPaid), notPastDue, cb.lessThanOrEqualTo(paid, BigDecimal.ZERO));
                case NOT_APPLICABLE -> notApplicable(root, cb);
            };
        };
    }

    private static Predicate notApplicable(Root<Sponsor> root, CriteriaBuilder cb) {
        return cb.or(cb.equal(root.get("sponsorshipStatus"), SponsorshipStatus.CANCELLED),
                cb.isNull(root.get("sponsorshipAmount")));
    }
}
