package com.eventtracker.controller;

import com.eventtracker.model.AlertRule;
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
