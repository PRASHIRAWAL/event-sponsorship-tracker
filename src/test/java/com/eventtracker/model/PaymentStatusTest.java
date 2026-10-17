package com.eventtracker.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    @ParameterizedTest(name = "amount {0}, paid {1}, due {2} days from today -> {3}")
    @CsvSource(nullValues = "null", value = {
            "10000, null, null, UNPAID",
            "10000, 0, 5, UNPAID",
            "10000, 2500, null, PARTIAL",
            "10000, 2500, 0, PARTIAL",
            "10000, 10000, null, PAID",
            "10000, 10000, -30, PAID",
            "10000, 0, -1, OVERDUE",
            "10000, 9999.99, -1, OVERDUE",
            "null, null, -1, NOT_APPLICABLE",
    })
    void derivesStatusFromAmountPaymentsAndDueDate(String amount, String paid, Integer dueInDays, PaymentStatus expected) {
        assertThat(PaymentStatus.of(SponsorshipStatus.CONFIRMED,
                amount == null ? null : new BigDecimal(amount),
                paid == null ? null : new BigDecimal(paid),
                dueInDays == null ? null : TODAY.plusDays(dueInDays),
                TODAY)).isEqualTo(expected);
    }

    @Test
    void cancelledSponsorshipHasNothingToCollect() {
        assertThat(PaymentStatus.of(SponsorshipStatus.CANCELLED, new BigDecimal("10000"), BigDecimal.ZERO,
                TODAY.minusDays(10), TODAY)).isEqualTo(PaymentStatus.NOT_APPLICABLE);
    }

    @Test
    void sponsorReportsOutstandingAmountAndPaidPercent() {
        Sponsor sponsor = new Sponsor("Acme", SponsorshipStatus.CONFIRMED);
        sponsor.setSponsorshipAmount(new BigDecimal("8000"));
        sponsor.setAmountPaid(new BigDecimal("2000"));

        assertThat(sponsor.getOutstandingAmount()).isEqualByComparingTo("6000");
        assertThat(sponsor.getPaidPercent()).isEqualTo(25);

        sponsor.setAmountPaid(null);
        assertThat(sponsor.getAmountPaid()).isEqualByComparingTo("0");
        assertThat(sponsor.getOutstandingAmount()).isEqualByComparingTo("8000");
    }

    @Test
    void filterableStatusesExcludeNotApplicable() {
        assertThat(PaymentStatus.filterable()).doesNotContain(PaymentStatus.NOT_APPLICABLE).hasSize(4);
    }
}
