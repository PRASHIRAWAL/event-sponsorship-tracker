package com.eventtracker.controller;

import com.eventtracker.model.AlertRule;
import com.eventtracker.model.EventStatus;
import com.eventtracker.service.AlertService;
import com.eventtracker.service.DashboardService;
import com.eventtracker.service.EventService;
import com.eventtracker.service.SponsorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private static final int PREVIEW_SIZE = 5;

    private final DashboardService dashboardService;
    private final AlertService alertService;
    private final EventService eventService;
    private final SponsorService sponsorService;

    public DashboardController(DashboardService dashboardService, AlertService alertService,
                               EventService eventService, SponsorService sponsorService) {
        this.dashboardService = dashboardService;
        this.alertService = alertService;
        this.eventService = eventService;
        this.sponsorService = sponsorService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("summary", dashboardService.getSummary());
        // Short previews for the dashboard panels; the full lists live on /events and /alerts
        model.addAttribute("upcomingEvents", eventService.search(null, EventStatus.UPCOMING).stream()
                .limit(PREVIEW_SIZE)
                .toList());
        model.addAttribute("topAlerts", alertService.findAlerts().stream()
                .limit(PREVIEW_SIZE)
                .toList());
        if (q != null && !q.isBlank()) {
            model.addAttribute("q", q);
            model.addAttribute("eventResults", eventService.search(q, null));
            model.addAttribute("sponsorResults", sponsorService.search(q, null, null));
        }
        return "dashboard";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("alerts", alertService.findAlerts());
        model.addAttribute("rules", AlertRule.values());
        return "alerts";
    }
}
