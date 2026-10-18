package com.eventtracker.controller;

import com.eventtracker.service.AlertService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the open alert count shown as a badge in the top navigation bar of every HTML page. */
@ControllerAdvice(assignableTypes = {DashboardController.class, EventController.class, SponsorController.class})
public class NavigationModelAdvice {

    private final AlertService alertService;

    public NavigationModelAdvice(AlertService alertService) {
        this.alertService = alertService;
    }

    @ModelAttribute("navAlertCount")
    public int navAlertCount() {
        return alertService.findAlerts().size();
    }
}
