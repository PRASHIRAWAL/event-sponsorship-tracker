package com.eventtracker.controller;

import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.service.EventService;
import com.eventtracker.service.InvalidEventReferenceException;
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

@Controller
@RequestMapping("/sponsors")
public class SponsorController {

    private static final String FORM_VIEW = "sponsors/form";

    private final SponsorService sponsorService;
    private final EventService eventService;

    public SponsorController(SponsorService sponsorService, EventService eventService) {
        this.sponsorService = sponsorService;
        this.eventService = eventService;
    }

    @GetMapping
    public String listSponsors(@RequestParam(required = false) String q,
                               @RequestParam(required = false) SponsorshipStatus status,
                               @RequestParam(required = false) Long eventId,
                               Model model) {
        model.addAttribute("sponsors", sponsorService.search(q, status, eventId));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("eventId", eventId);
        model.addAttribute("statuses", SponsorshipStatus.values());
        model.addAttribute("events", eventService.findAll());
        model.addAttribute("filtered", (q != null && !q.isBlank()) || status != null || eventId != null);
        return "sponsors/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long eventId, Model model) {
        SponsorForm form = new SponsorForm();
        form.setSponsorshipStatus(SponsorshipStatus.PROPOSED);
        form.setEventId(eventId);
        return showForm(form, null, model);
    }

    @PostMapping
    public String createSponsor(@Valid @ModelAttribute("sponsor") SponsorForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(form, null, model);
        }
        try {
            Sponsor created = sponsorService.create(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Sponsor \"" + created.getName() + "\" created successfully.");
            return "redirect:/sponsors";
        } catch (InvalidEventReferenceException ex) {
            bindingResult.rejectValue("eventId", "invalid", InvalidEventReferenceException.MESSAGE);
            return showForm(form, null, model);
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        return showForm(SponsorForm.from(sponsorService.findById(id)), id, model);
    }

    @PostMapping("/{id}")
    public String updateSponsor(@PathVariable Long id,
                                @Valid @ModelAttribute("sponsor") SponsorForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(form, id, model);
        }
        try {
            Sponsor updated = sponsorService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Sponsor \"" + updated.getName() + "\" updated successfully.");
            return "redirect:/sponsors";
        } catch (InvalidEventReferenceException ex) {
            bindingResult.rejectValue("eventId", "invalid", InvalidEventReferenceException.MESSAGE);
            return showForm(form, id, model);
        }
    }

    private String showForm(SponsorForm form, Long sponsorId, Model model) {
        model.addAttribute("sponsor", form);
        model.addAttribute("sponsorId", sponsorId);
        model.addAttribute("statuses", SponsorshipStatus.values());
        model.addAttribute("events", eventService.findAll());
        return FORM_VIEW;
    }
}
