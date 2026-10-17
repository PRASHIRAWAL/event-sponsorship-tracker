package com.eventtracker.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Funding figures for one event.
 *
 * @param committed    total of Confirmed and Completed sponsorships
 * @param pipeline     total of Proposed and Pending sponsorships
 * @param collected    payments received from committed sponsors
 * @param sponsorCount sponsors linked to the event, excluding cancelled ones
 */
public record EventFunding(Event event, BigDecimal committed, BigDecimal pipeline, BigDecimal collected,
                           long sponsorCount) {

    public BigDecimal target() {
        return event.getSponsorshipTarget();
    }

    public boolean hasTarget() {
        return target() != null && target().signum() > 0;
    }

    /** Committed amount as a percentage of the target (0 when no target is set). Can exceed 100. */
    public int percentOfTarget() {
        if (!hasTarget()) {
            return 0;
        }
        return committed.multiply(BigDecimal.valueOf(100)).divide(target(), 0, RoundingMode.DOWN).intValue();
    }

    /** Width of the progress bar, capped at 100%. */
    public int progressWidth() {
        return Math.min(100, percentOfTarget());
    }

    /** Amount still needed to reach the target (never negative). */
    public BigDecimal remainingToTarget() {
        if (!hasTarget()) {
            return BigDecimal.ZERO;
        }
        return target().subtract(committed).max(BigDecimal.ZERO);
    }

    /** Committed amount not yet received. */
    public BigDecimal outstanding() {
        return committed.subtract(collected).max(BigDecimal.ZERO);
    }
}
