package com.eventtracker.service;

import com.eventtracker.model.ActivityType;
import com.eventtracker.model.Alert;
import com.eventtracker.model.Payment;
import com.eventtracker.model.PaymentForm;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.repository.PaymentRepository;
import com.eventtracker.repository.SponsorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Records payments against sponsorships and keeps each sponsor's amount paid in step with its payments. */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SponsorRepository sponsorRepository;
    private final ActivityService activityService;

    public PaymentService(PaymentRepository paymentRepository, SponsorRepository sponsorRepository,
                          ActivityService activityService) {
        this.paymentRepository = paymentRepository;
        this.sponsorRepository = sponsorRepository;
        this.activityService = activityService;
    }

    public List<Payment> findBySponsor(Long sponsorId) {
        return paymentRepository.findBySponsorIdOrderByPaymentDateDescIdDesc(sponsorId);
    }

    /**
     * Rules: the sponsorship must not be cancelled, must have an amount, and the payment may not be
     * larger than the outstanding balance.
     */
    @Transactional
    public Payment record(Long sponsorId, PaymentForm form) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new SponsorNotFoundException(sponsorId));
        if (sponsor.getSponsorshipStatus() == SponsorshipStatus.CANCELLED) {
            throw new PaymentNotAllowedException("amount", "Payments cannot be recorded for a cancelled sponsorship");
        }
        if (sponsor.getSponsorshipAmount() == null) {
            throw new PaymentNotAllowedException("amount",
                    "Record a sponsorship amount for this sponsor before adding payments");
        }
        BigDecimal outstanding = sponsor.getOutstandingAmount();
        if (form.getAmount().compareTo(outstanding) > 0) {
            throw new PaymentNotAllowedException("amount",
                    "Payment exceeds the outstanding balance of " + Money.format(outstanding));
        }

        Payment payment = paymentRepository.save(new Payment(sponsor, form.getAmount(), form.getPaymentDate(),
                form.getMethod(), blankToNull(form.getReference())));
        refreshAmountPaid(sponsor);
        activityService.record(ActivityType.PAYMENT_RECORDED, Alert.SPONSOR, sponsor.getId(),
                "Payment of " + Money.format(form.getAmount()) + " (" + form.getMethod().getLabel()
                        + ") recorded for " + sponsor.getName());
        return payment;
    }

    /** Removes a payment (for example one entered by mistake) and returns the sponsor it belonged to. */
    @Transactional
    public Sponsor delete(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        Sponsor sponsor = payment.getSponsor();
        paymentRepository.delete(payment);
        paymentRepository.flush();
        refreshAmountPaid(sponsor);
        activityService.record(ActivityType.PAYMENT_REMOVED, Alert.SPONSOR, sponsor.getId(),
                "Payment of " + Money.format(payment.getAmount()) + " removed from " + sponsor.getName());
        return sponsor;
    }

    /** Recalculates the stored total from the payments table, so it can never drift from the payments. */
    private void refreshAmountPaid(Sponsor sponsor) {
        paymentRepository.flush();
        sponsor.setAmountPaid(paymentRepository.totalForSponsor(sponsor.getId()));
        sponsorRepository.save(sponsor);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
