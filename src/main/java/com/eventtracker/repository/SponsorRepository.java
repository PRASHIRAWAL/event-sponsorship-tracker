package com.eventtracker.repository;

import com.eventtracker.model.Sponsor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The linked event is loaded together with the sponsor (entity graph) so pages and
 * API responses can show the event name without extra queries.
 */
@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long>, JpaSpecificationExecutor<Sponsor> {

    @Override
    @EntityGraph(attributePaths = "event")
    List<Sponsor> findAll();

    @Override
    @EntityGraph(attributePaths = "event")
    List<Sponsor> findAll(Specification<Sponsor> specification, Sort sort);

    @Override
    @EntityGraph(attributePaths = "event")
    Page<Sponsor> findAll(Specification<Sponsor> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "event")
    Optional<Sponsor> findById(Long id);

    long countByEventId(Long eventId);

    /** Used before deleting an event, so its sponsors are kept but no longer linked to it. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Sponsor s set s.event = null where s.event.id = :eventId")
    int unlinkFromEvent(@Param("eventId") Long eventId);
}
