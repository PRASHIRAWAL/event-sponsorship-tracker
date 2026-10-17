package com.eventtracker.controller;

import com.eventtracker.model.Alert;
import com.eventtracker.model.Event;
import com.eventtracker.model.EventFunding;
import com.eventtracker.model.EventStatus;
import com.eventtracker.model.Sponsor;
import com.eventtracker.service.ActivityService;
import com.eventtracker.service.CsvExporter;
import com.eventtracker.service.EventService;
import com.eventtracker.service.FundingCalculator;
import com.eventtracker.service.ReportService;
import com.eventtracker.service.SponsorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/events")
public class EventController {

    private static final String FORM_VIEW = "events/form";

    /** Sortable columns: request key -> entity property. The first entry is the default sort. */
    private static final Map<String, String> SORT_KEYS = new LinkedHashMap<>();

    static {
        SORT_KEYS.put("date", "eventDate");
        SORT_KEYS.put("name", "name");
        SORT_KEYS.put("venue", "venue");
        SORT_KEYS.put("target", "sponsorshipTarget");
    }

    private final EventService eventService;
    private final SponsorService sponsorService;
    private final ReportService reportService;
    private final ActivityService activityService;

    public EventController(EventService eventService, SponsorService sponsorService, ReportService reportService,
                           ActivityService activityService) {
        this.eventService = eventService;
        this.sponsorService = sponsorService;
        this.reportService = reportService;
        this.activityService = activityService;
    }

    @GetMapping
    public String listEvents(@RequestParam(required = false) String q,
                             @RequestParam(required = false) EventStatus status,
                             @RequestParam(required = false) Integer page,
                             @RequestParam(required = false) Integer size,
                             @RequestParam(required = false) String sort,
                             @RequestParam(required = false) String dir,
                             Model model) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("q", q);
        filters.put("status", status);
        PageView pager = PageView.of("/events", SORT_KEYS, sort, dir, size, filters, true);
        Page<Event> events = eventService.searchPage(q, status, pager.pageable(page));
        if (PageView.isBeyondLastPage(events)) {
            events = eventService.searchPage(q, status, pager.pageable(events.getTotalPages() - 1));
        }

        model.addAttribute("events", events.getContent());
        model.addAttribute("funding", reportService.fundingFor(events.getContent()));
        model.addAttribute("pager", pager.withPage(events));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", EventStatus.values());
        model.addAttribute("filtered", (q != null && !q.isBlank()) || status != null);
        return "events/list";
    }

    /** CSV of the events matching the current filters (all pages), with their funding figures. */
    @GetMapping("/export.csv")
    public ResponseEntity<String> exportEvents(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) EventStatus status) {
        List<Event> events = eventService.search(q, status);
        List<EventFunding> funding = List.copyOf(reportService.fundingFor(events).values());
        return csv("events-" + LocalDate.now() + ".csv", CsvExporter.events(funding));
    }

    @GetMapping("/{id}")
    public String showEvent(@PathVariable Long id, Model model) {
        Event event = eventService.findById(id);
        List<Sponsor> sponsors = sponsorService.findByEvent(id);
        EventFunding funding = FundingCalculator.forEvent(event, sponsors);
        model.addAttribute("event", event);
        model.addAttribute("sponsors", sponsors);
        model.addAttribute("funding", funding);
        model.addAttribute("committedAmount", funding.committed());
        model.addAttribute("tiers", FundingCalculator.byTier(sponsors).stream()
                .filter(tier -> tier.count() > 0)
                .toList());
        model.addAttribute("activity", activityService.forSubject(Alert.EVENT, id));
        return "events/detail";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("event", new Event());
        return FORM_VIEW;
    }

    @PostMapping
    public String createEvent(@Valid @ModelAttribute("event") Event event,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        Event created = eventService.create(event);
        redirectAttributes.addFlashAttribute("successMessage",
                "Event \"" + created.getName() + "\" created successfully.");
        return "redirect:/events";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.findById(id));
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String updateEvent(@PathVariable Long id,
                              @Valid @ModelAttribute("event") Event event,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            event.setId(id);
            return FORM_VIEW;
        }
        Event updated = eventService.update(id, event);
        redirectAttributes.addFlashAttribute("successMessage",
                "Event \"" + updated.getName() + "\" updated successfully.");
        return "redirect:/events";
    }

    @PostMapping("/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String name = eventService.findById(id).getName();
        int unlinked = eventService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Event \"" + name + "\" deleted."
                + (unlinked == 0 ? "" : " " + unlinked + " sponsor(s) were kept and are no longer linked to an event."));
        return "redirect:/events";
    }

    static ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                // Byte order mark so Excel opens UTF-8 names (e.g. café, ₹) correctly
                .body("﻿" + body);
    }
}
