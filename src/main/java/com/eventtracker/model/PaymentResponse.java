package com.eventtracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** JSON representation of a payment returned by the REST API. */
public record PaymentResponse(Long id, Long sponsorId, BigDecimal amount, LocalDate paymentDate,
                              PaymentMethod method, String reference) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getSponsor().getId(), payment.getAmount(),
                payment.getPaymentDate(), payment.getMethod(), payment.getReference());
    }
}
