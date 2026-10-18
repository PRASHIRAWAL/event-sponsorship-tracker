package com.eventtracker.controller;

import com.eventtracker.model.Alert;
import com.eventtracker.model.AlertRule;
import com.eventtracker.model.DashboardSummary;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.TierSummary;
import com.eventtracker.service.ActivityService;
import com.eventtracker.service.AlertService;
import com.eventtracker.service.DashboardService;
import com.eventtracker.service.EventService;
import com.eventtracker.service.ReportService;
import com.eventtracker.service.SponsorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Controller
public class DashboardController {

    private static final int PREVIEW_SIZE = 5;
    private static final int ACTIVITY_PAGE_SIZE = 20;

    private final DashboardService dashboardService;
    private final AlertService alertService;
    private final EventService eventService;
    private final SponsorService sponsorService;
    private final ActivityService activityService;
    private final ReportService reportService;

    public DashboardController(DashboardService dashboardService, AlertService alertService,
                               EventService eventService, SponsorService sponsorService,
                               ActivityService activityService, ReportService reportService) {
        this.dashboardService = dashboardService;
        this.alertService = alertService;
        this.eventService = eventService;
        this.sponsorService = sponsorService;
        this.activityService = activityService;
        this.reportService = reportService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String q, Model model) {
        DashboardSummary summary = dashboardService.getSummary();
        model.addAttribute("summary", summary);
        model.addAttribute("statusDonut", Charts.statusDonut(summary.sponsorsByStatus()));
        // Short previews for the dashboard panels; the full lists live on /events, /alerts and /activity
        List<EventFunding> upcomingFunding = dashboardService.upcomingFunding(PREVIEW_SIZE);
        model.addAttribute("upcomingFunding", upcomingFunding);
        model.addAttribute("upcomingEvents", upcomingFunding.stream().map(EventFunding::event).toList());
        model.addAttribute("topAlerts", alertService.findAlerts().stream()
                .limit(PREVIEW_SIZE)
                .toList());
        model.addAttribute("recentActivity", activityService.recent(6));
        if (q != null && !q.isBlank()) {
            model.addAttribute("q", q);
            model.addAttribute("eventResults", eventService.search(q, null));
            model.addAttribute("sponsorResults", sponsorService.search(q, null, null));
        }
        return "dashboard";
    }

    @GetMapping("/alerts")
    public String alerts(@RequestParam(required = false) AlertRule.Severity severity, Model model) {
        List<Alert> all = alertService.findAlerts();
        model.addAttribute("allCount", all.size());
        model.addAttribute("alerts", severity == null ? all
                : all.stream().filter(alert -> alert.rule().getSeverity() == severity).toList());
        model.addAttribute("severity", severity);
        model.addAttribute("severities", AlertRule.Severity.values());
        model.addAttribute("severityCounts", Arrays.stream(AlertRule.Severity.values())
                .map(level -> all.stream().filter(alert -> alert.rule().getSeverity() == level).count())
                .toList());
        model.addAttribute("rules", AlertRule.values());
        return "alerts";
    }

    @GetMapping("/activity")
    public String activity(@RequestParam(required = false) String type,
                           @RequestParam(required = false) Integer page,
                           Model model) {
        String subjectType = Alert.EVENT.equals(type) || Alert.SPONSOR.equals(type) ? type : null;
        int number = page == null || page < 0 ? 0 : page;
        var entries = activityService.page(subjectType, PageRequest.of(number, ACTIVITY_PAGE_SIZE));
        model.addAttribute("entries", entries);
        model.addAttribute("type", subjectType);
        return "activity";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        List<EventFunding> funding = reportService.eventFunding();
        List<TierSummary> tiers = reportService.tierBreakdown();
        List<ReportService.MethodTotal> methods = reportService.paymentMethods();
        model.addAttribute("summary", dashboardService.getSummary());
        model.addAttribute("funding", funding);
        model.addAttribute("fundingTotals", totals(funding));
        model.addAttribute("fundingMax", funding.stream()
                .map(row -> row.hasTarget() ? row.target().max(row.committed()) : row.committed())
                .reduce(BigDecimal.ZERO, BigDecimal::max));
        model.addAttribute("tiers", tiers);
        model.addAttribute("tierDonut", Charts.tierDonut(tiers));
        model.addAttribute("tierTotal", tiers.stream().mapToLong(TierSummary::count).sum());
        model.addAttribute("methods", methods);
        model.addAttribute("methodMax", methods.stream()
                .map(ReportService.MethodTotal::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::max));
        model.addAttribute("charts", CHARTS);
        model.addAttribute("topSponsors", reportService.topSponsors(5));
        return "reports";
    }

    /** Column totals for the event funding report: target, committed, pipeline, collected, outstanding. */
    private static Map<String, BigDecimal> totals(List<EventFunding> funding) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("target", sum(funding, row -> row.hasTarget() ? row.target() : BigDecimal.ZERO));
        totals.put("committed", sum(funding, EventFunding::committed));
        totals.put("pipeline", sum(funding, EventFunding::pipeline));
        totals.put("collected", sum(funding, EventFunding::collected));
        totals.put("outstanding", sum(funding, EventFunding::outstanding));
        return totals;
    }

    private static BigDecimal sum(List<EventFunding> rows, Function<EventFunding, BigDecimal> value) {
        return rows.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Lets templates call the static chart helpers, e.g. ${charts.width(value, max)}. */
    public static final class ChartHelper {
        public int width(BigDecimal value, BigDecimal max) {
            return Charts.width(value, max);
        }
    }

    private static final ChartHelper CHARTS = new ChartHelper();
}
