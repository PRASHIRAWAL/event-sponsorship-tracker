package com.eventtracker.controller;

import com.eventtracker.model.Event;
import com.eventtracker.model.EventStatus;
import com.eventtracker.model.Sponsor;
import com.eventtracker.service.EventService;
import com.eventtracker.service.SponsorService;
import jakarta.validation.Valid;
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

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/events")
public class EventController {

    private static final String FORM_VIEW = "events/form";

    private final EventService eventService;
    private final SponsorService sponsorService;

    public EventController(EventService eventService, SponsorService sponsorService) {
        this.eventService = eventService;
        this.sponsorService = sponsorService;
    }

    @GetMapping
    public String listEvents(@RequestParam(required = false) String q,
                             @RequestParam(required = false) EventStatus status,
                             Model model) {
        model.addAttribute("events", eventService.search(q, status));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", EventStatus.values());
        model.addAttribute("filtered", (q != null && !q.isBlank()) || status != null);
        return "events/list";
    }

    @GetMapping("/{id}")
    public String showEvent(@PathVariable Long id, Model model) {
        Event event = eventService.findById(id);
        List<Sponsor> sponsors = sponsorService.findByEvent(id);
        BigDecimal committedAmount = sponsors.stream()
                .filter(sponsor -> sponsor.getSponsorshipStatus().isCommitted())
                .map(Sponsor::getSponsorshipAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("event", event);
        model.addAttribute("sponsors", sponsors);
        model.addAttribute("committedAmount", committedAmount);
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
}
