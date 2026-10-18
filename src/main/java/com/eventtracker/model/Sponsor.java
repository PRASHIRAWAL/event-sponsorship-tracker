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
import java.time.LocalDate;

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

    @Column(name = "industry", length = 80)
    private String industry;

    @Column(name = "sponsorship_amount", precision = 14, scale = 2)
    private BigDecimal sponsorshipAmount;

    // Stored as VARCHAR (not a MySQL ENUM) so new statuses can be added without altering the column
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sponsorship_status", nullable = false, length = 20)
    private SponsorshipStatus sponsorshipStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sponsorship_tier", length = 20)
    private SponsorshipTier tier;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    /**
     * Sum of all recorded payments. Kept on the sponsor (and updated by PaymentService) so lists, totals
     * and the payment status filter do not need to load every payment. Null on rows created before payments existed.
     */
    @Column(name = "amount_paid", precision = 14, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "notes", length = 500)
    private String notes;

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

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
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

    public SponsorshipTier getTier() {
        return tier;
    }

    public void setTier(SponsorshipTier tier) {
        this.tier = tier;
    }

    public LocalDate getPaymentDueDate() {
        return paymentDueDate;
    }

    public void setPaymentDueDate(LocalDate paymentDueDate) {
        this.paymentDueDate = paymentDueDate;
    }

    /** Never null: rows without payments report zero. */
    public BigDecimal getAmountPaid() {
        return amountPaid == null ? BigDecimal.ZERO : amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    /** Amount still to be received; zero when no amount is recorded or the sponsorship is cancelled. */
    public BigDecimal getOutstandingAmount() {
        if (sponsorshipAmount == null || sponsorshipStatus == SponsorshipStatus.CANCELLED) {
            return BigDecimal.ZERO;
        }
        return sponsorshipAmount.subtract(getAmountPaid()).max(BigDecimal.ZERO);
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatusOn(LocalDate.now());
    }

    public PaymentStatus paymentStatusOn(LocalDate today) {
        return PaymentStatus.of(sponsorshipStatus, sponsorshipAmount, getAmountPaid(), paymentDueDate, today);
    }

    /** Percentage of the sponsorship amount already paid (0-100), for progress bars. */
    public int getPaidPercent() {
        if (sponsorshipAmount == null || sponsorshipAmount.signum() <= 0) {
            return 0;
        }
        int percent = getAmountPaid().multiply(BigDecimal.valueOf(100))
                .divide(sponsorshipAmount, 0, java.math.RoundingMode.DOWN).intValue();
        return Math.min(100, percent);
    }
}
