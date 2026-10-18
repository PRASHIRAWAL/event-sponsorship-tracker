package com.eventtracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @NotBlank(message = "Event name is required")
    @Size(max = 150, message = "Event name must be at most 150 characters")
    @Column(name = "event_name", nullable = false, length = 150)
    private String name;

    @NotNull(message = "Event date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @NotBlank(message = "Venue is required")
    @Size(max = 150, message = "Venue must be at most 150 characters")
    @Column(name = "venue", nullable = false, length = 150)
    private String venue;

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    /** Fundraising goal for the event; optional. Committed sponsorships are measured against it. */
    @DecimalMin(value = "0.01", message = "Sponsorship target must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Sponsorship target can have at most 12 digits and 2 decimals")
    @Column(name = "sponsorship_target", precision = 14, scale = 2)
    private BigDecimal sponsorshipTarget;

    public Event() {
    }

    public Event(String name, LocalDate eventDate, String venue, String description) {
        this.name = name;
        this.eventDate = eventDate;
        this.venue = venue;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSponsorshipTarget() {
        return sponsorshipTarget;
    }

    public void setSponsorshipTarget(BigDecimal sponsorshipTarget) {
        this.sponsorshipTarget = sponsorshipTarget;
    }

    /** Derived from the event date; not stored in the database and not part of the API response. */
    @JsonIgnore
    public EventStatus getStatus() {
        return EventStatus.of(eventDate, LocalDate.now());
    }

    /** Days from today until the event (negative for past events); not part of the API response. */
    @JsonIgnore
    public long getDaysUntil() {
        return LocalDate.now().until(eventDate, java.time.temporal.ChronoUnit.DAYS);
    }
}
