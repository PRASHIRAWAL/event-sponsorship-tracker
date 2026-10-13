package com.eventtracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "sponsors")
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sponsor_id")
    private Long id;

    @Column(name = "sponsor_name", nullable = false, length = 150)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "sponsorship_amount", precision = 14, scale = 2)
    private BigDecimal sponsorshipAmount;

    // Stored as VARCHAR (not a MySQL ENUM) so new statuses can be added without altering the column
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sponsorship_status", nullable = false, length = 20)
    private SponsorshipStatus sponsorshipStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    public Sponsor() {
    }

    public Sponsor(String name, SponsorshipStatus sponsorshipStatus) {
        this.name = name;
        this.sponsorshipStatus = sponsorshipStatus;
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

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getSponsorshipAmount() {
        return sponsorshipAmount;
    }

    public void setSponsorshipAmount(BigDecimal sponsorshipAmount) {
        this.sponsorshipAmount = sponsorshipAmount;
    }

    public SponsorshipStatus getSponsorshipStatus() {
        return sponsorshipStatus;
    }

    public void setSponsorshipStatus(SponsorshipStatus sponsorshipStatus) {
        this.sponsorshipStatus = sponsorshipStatus;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
