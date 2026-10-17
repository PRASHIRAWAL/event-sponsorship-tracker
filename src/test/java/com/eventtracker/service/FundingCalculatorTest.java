package com.eventtracker.service;

import com.eventtracker.model.Event;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import com.eventtracker.model.TierSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FundingCalculatorTest {

    @Test
    void separatesCommittedPipelineAndCollectedPerEvent() {
        Event fest = event(1L, "200000");
        Event meetup = event(2L, null);
        List<Sponsor> sponsors = List.of(
                sponsor(10L, SponsorshipStatus.CONFIRMED, "100000", "40000", fest, SponsorshipTier.GOLD),
                sponsor(11L, SponsorshipStatus.COMPLETED, "50000", "50000", fest, SponsorshipTier.SILVER),
                sponsor(12L, SponsorshipStatus.PROPOSED, "30000", null, fest, null),
                sponsor(13L, SponsorshipStatus.CANCELLED, "99999", null, fest, SponsorshipTier.GOLD),
                sponsor(14L, SponsorshipStatus.PENDING, "5000", null, meetup, null));

        Map<Long, EventFunding> funding = FundingCalculator.byEvent(List.of(fest, meetup), sponsors);

        EventFunding festFunding = funding.get(1L);
        assertThat(festFunding.committed()).isEqualByComparingTo("150000");
        assertThat(festFunding.pipeline()).isEqualByComparingTo("30000");
        assertThat(festFunding.collected()).isEqualByComparingTo("90000");
        assertThat(festFunding.outstanding()).isEqualByComparingTo("60000");
        assertThat(festFunding.sponsorCount()).isEqualTo(3);
        assertThat(festFunding.percentOfTarget()).isEqualTo(75);
        assertThat(festFunding.remainingToTarget()).isEqualByComparingTo("50000");

        EventFunding meetupFunding = funding.get(2L);
        assertThat(meetupFunding.hasTarget()).isFalse();
        assertThat(meetupFunding.percentOfTarget()).isZero();
        assertThat(meetupFunding.pipeline()).isEqualByComparingTo("5000");
    }

    @Test
    void progressIsCappedButPercentCanExceedTarget() {
        Event event = event(1L, "1000");
        EventFunding funding = FundingCalculator.forEvent(event,
                List.of(sponsor(10L, SponsorshipStatus.CONFIRMED, "1500", null, event, null)));

        assertThat(funding.percentOfTarget()).isEqualTo(150);
        assertThat(funding.progressWidth()).isEqualTo(100);
        assertThat(funding.remainingToTarget()).isEqualByComparingTo("0");
    }

    @Test
    void tierBreakdownSkipsCancelledAndIncludesNoTierRow() {
        List<TierSummary> tiers = FundingCalculator.byTier(List.of(
                sponsor(10L, SponsorshipStatus.CONFIRMED, "100", null, null, SponsorshipTier.GOLD),
                sponsor(11L, SponsorshipStatus.PROPOSED, "50", null, null, SponsorshipTier.GOLD),
                sponsor(12L, SponsorshipStatus.CANCELLED, "70", null, null, SponsorshipTier.GOLD),
                sponsor(13L, SponsorshipStatus.PENDING, null, null, null, null)));

        assertThat(tiers).hasSize(SponsorshipTier.values().length + 1);
        TierSummary gold = tiers.stream().filter(row -> row.tier() == SponsorshipTier.GOLD).findFirst().orElseThrow();
        assertThat(gold.count()).isEqualTo(2);
        assertThat(gold.totalAmount()).isEqualByComparingTo("150");
        TierSummary none = tiers.get(tiers.size() - 1);
        assertThat(none.tier()).isNull();
        assertThat(none.label()).isEqualTo("No tier");
        assertThat(none.count()).isEqualTo(1);
    }

    private static Event event(Long id, String target) {
        Event event = new Event("Event " + id, LocalDate.of(2099, 1, 1), "Venue", null);
        event.setId(id);
        event.setSponsorshipTarget(target == null ? null : new BigDecimal(target));
        return event;
    }

    private static Sponsor sponsor(Long id, SponsorshipStatus status, String amount, String paid, Event event,
                                   SponsorshipTier tier) {
        Sponsor sponsor = new Sponsor("Sponsor " + id, status);
        sponsor.setId(id);
        sponsor.setSponsorshipAmount(amount == null ? null : new BigDecimal(amount));
        sponsor.setAmountPaid(paid == null ? null : new BigDecimal(paid));
        sponsor.setEvent(event);
        sponsor.setTier(tier);
        return sponsor;
    }
}
