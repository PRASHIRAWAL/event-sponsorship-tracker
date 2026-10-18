package com.eventtracker.controller;

import com.eventtracker.model.ActivityLog;
import com.eventtracker.model.ActivityType;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.TierSummary;
import com.eventtracker.service.ActivityService;
import com.eventtracker.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only reporting and audit endpoints. */
@RestController
@RequestMapping("/api")
public class ReportApiController {

    private static final int MAX_ACTIVITY = 100;

    /** Flat JSON view of {@link EventFunding}. */
    public record FundingResponse(Long eventId, String eventName, LocalDate eventDate, BigDecimal target,
                                  BigDecimal committed, BigDecimal pipeline, BigDecimal collected,
                                  BigDecimal outstanding, Integer percentOfTarget, long activeSponsors) {

        static FundingResponse from(EventFunding funding) {
            return new FundingResponse(funding.event().getId(), funding.event().getName(),
                    funding.event().getEventDate(), funding.target(), funding.committed(), funding.pipeline(),
                    funding.collected(), funding.outstanding(),
                    funding.hasTarget() ? funding.percentOfTarget() : null, funding.sponsorCount());
        }
    }

    public record ActivityResponse(Long id, LocalDateTime occurredAt, ActivityType type, String subjectType,
                                   Long subjectId, String message) {

        static ActivityResponse from(ActivityLog entry) {
            return new ActivityResponse(entry.getId(), entry.getOccurredAt(), entry.getType(),
                    entry.getSubjectType(), entry.getSubjectId(), entry.getMessage());
        }
    }

    private final ReportService reportService;
    private final ActivityService activityService;

    public ReportApiController(ReportService reportService, ActivityService activityService) {
        this.reportService = reportService;
        this.activityService = activityService;
    }

    @GetMapping("/reports/events")
    public List<FundingResponse> eventFunding() {
        return reportService.eventFunding().stream().map(FundingResponse::from).toList();
    }

    @GetMapping("/reports/tiers")
    public List<TierSummary> tiers() {
        return reportService.tierBreakdown();
    }

    @GetMapping("/reports/payment-methods")
    public List<ReportService.MethodTotal> paymentMethods() {
        return reportService.paymentMethods();
    }

    /** The most recent changes, newest first (limit 1-100, default 20). */
    @GetMapping("/activity")
    public List<ActivityResponse> activity(@RequestParam(defaultValue = "20") int limit) {
        int size = Math.max(1, Math.min(MAX_ACTIVITY, limit));
        return activityService.recent(size).stream().map(ActivityResponse::from).toList();
    }
}
