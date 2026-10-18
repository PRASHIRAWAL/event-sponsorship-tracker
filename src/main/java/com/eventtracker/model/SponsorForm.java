package com.eventtracker.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input object for creating and updating sponsors, used by both the web form and the REST API.
 * Keeping it separate from the {@link Sponsor} entity lets clients refer to an event by its id
 * and keeps the amount paid (which only changes through payments) out of client input.
 */
public class SponsorForm {

    @NotBlank(message = "Sponsor name is required")
    @Size(max = 150, message = "Sponsor name must be at most 150 characters")
    private String name;

    @Size(max = 100, message = "Contact person must be at most 100 characters")
    private String contactPerson;

    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9 ()-]{7,20}$",
            message = "Enter a valid phone number (7-20 digits, spaces, +, - or brackets)")
    private String phone;

    @Size(max = 80, message = "Industry must be at most 80 characters")
    private String industry;

    @DecimalMin(value = "0.01", message = "Sponsorship amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Sponsorship amount can have at most 12 digits and 2 decimals")
    private BigDecimal sponsorshipAmount;

    @NotNull(message = "Sponsorship status is required")
    private SponsorshipStatus sponsorshipStatus;

    private SponsorshipTier tier;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDueDate;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;

    private Long eventId;

    public static SponsorForm from(Sponsor sponsor) {
        SponsorForm form = new SponsorForm();
        form.setName(sponsor.getName());
        form.setContactPerson(sponsor.getContactPerson());
        form.setEmail(sponsor.getEmail());
        form.setPhone(sponsor.getPhone());
        form.setIndustry(sponsor.getIndustry());
        form.setSponsorshipAmount(sponsor.getSponsorshipAmount());
        form.setSponsorshipStatus(sponsor.getSponsorshipStatus());
        form.setTier(sponsor.getTier());
        form.setPaymentDueDate(sponsor.getPaymentDueDate());
        form.setNotes(sponsor.getNotes());
        form.setEventId(sponsor.getEvent() == null ? null : sponsor.getEvent().getId());
        return form;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}
