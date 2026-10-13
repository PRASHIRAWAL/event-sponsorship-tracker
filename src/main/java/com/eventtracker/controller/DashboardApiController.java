package com.eventtracker.controller;

import com.eventtracker.model.Alert;
import com.eventtracker.model.DashboardSummary;
import com.eventtracker.service.AlertService;
import com.eventtracker.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final AlertService alertService;

    public DashboardApiController(DashboardService dashboardService, AlertService alertService) {
        this.dashboardService = dashboardService;
        this.alertService = alertService;
    }

    @GetMapping("/summary")
    public DashboardSummary getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/exceptions")
    public List<Alert> getExceptions() {
        return alertService.findAlerts();
    }
}
