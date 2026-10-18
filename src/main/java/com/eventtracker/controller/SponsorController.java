package com.eventtracker.controller;

import com.eventtracker.model.Alert;
import com.eventtracker.model.PaymentForm;
import com.eventtracker.model.PaymentMethod;
import com.eventtracker.model.PaymentStatus;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorFilter;
import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import com.eventtracker.service.ActivityService;
import com.eventtracker.service.CsvExporter;
import com.eventtracker.service.EventService;
import com.eventtracker.service.InvalidEventReferenceException;
import com.eventtracker.service.InvalidSponsorAmountException;
import com.eventtracker.service.Money;
import com.eventtracker.service.PaymentNotAllowedException;
import com.eventtracker.service.PaymentService;
import com.eventtracker.service.SponsorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
import java.util.Map;

@Controller
@RequestMapping("/sponsors")
public class SponsorController {

    private static final String FORM_VIEW = "sponsors/form";
    private static final String DETAIL_VIEW = "sponsors/detail";

    /** Sortable columns: request key -> entity property. The first entry is the default sort. */
    private static final Map<String, String> SORT_KEYS = new LinkedHashMap<>();

    static {
        SORT_KEYS.put("name", "name");
        SORT_KEYS.put("amount", "sponsorshipAmount");
        SORT_KEYS.put("paid", "amountPaid");
        SORT_KEYS.put("status", "sponsorshipStatus");
        SORT_KEYS.put("due", "paymentDueDate");
    }

    private final SponsorService sponsorService;
    private final EventService eventService;
    private final PaymentService paymentService;
    private final ActivityService activityService;

    public SponsorController(SponsorService sponsorService, EventService eventService,
                             PaymentService paymentService, ActivityService activityService) {
        this.sponsorService = sponsorService;
        this.eventService = eventService;
        this.paymentService = paymentService;
        this.activityService = activityService;
    }

    @GetMapping
    public String listSponsors(@RequestParam(required = false) String q,
                               @RequestParam(required = false) SponsorshipStatus status,
                               @RequestParam(required = false) Long eventId,
                               @RequestParam(required = false) SponsorshipTier tier,
                               @RequestParam(required = false) PaymentStatus payment,
                               @RequestParam(required = false) Integer page,
                               @RequestParam(required = false) Integer size,
                               @RequestParam(required = false) String sort,
                               @RequestParam(required = false) String dir,
                               Model model) {
        SponsorFilter filter = new SponsorFilter(q, status, eventId, tier, payment);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("q", q);
        filters.put("status", status);
        filters.put("eventId", eventId);
        filters.put("tier", tier);
        filters.put("payment", payment);
        PageView pager = PageView.of("/sponsors", SORT_KEYS, sort, dir, size, filters, true);
        Page<Sponsor> sponsors = sponsorService.searchPage(filter, pager.pageable(page));
        if (PageView.isBeyondLastPage(sponsors)) {
            sponsors = sponsorService.searchPage(filter, pager.pageable(sponsors.getTotalPages() - 1));
        }

        model.addAttribute("sponsors", sponsors.getContent());
        model.addAttribute("pager", pager.withPage(sponsors));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("eventId", eventId);
        model.addAttribute("tier", tier);
        model.addAttribute("payment", payment);
        model.addAttribute("statuses", SponsorshipStatus.values());
        model.addAttribute("tiers", SponsorshipTier.values());
        model.addAttribute("paymentStatuses", PaymentStatus.filterable());
        model.addAttribute("events", eventService.findAll());
        model.addAttribute("filtered", filter.isActive());
        return "sponsors/list";
    }

    /** CSV of the sponsors matching the current filters (all pages). */
    @GetMapping("/export.csv")
    public ResponseEntity<String> exportSponsors(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) SponsorshipStatus status,
                                                 @RequestParam(required = false) Long eventId,
                                                 @RequestParam(required = false) SponsorshipTier tier,
                                                 @RequestParam(required = false) PaymentStatus payment) {
        var sponsors = sponsorService.search(new SponsorFilter(q, status, eventId, tier, payment));
        return EventController.csv("sponsors-" + LocalDate.now() + ".csv", CsvExporter.sponsors(sponsors));
    }

    @GetMapping("/{id}")
    public String showSponsor(@PathVariable Long id, Model model) {
        PaymentForm payment = new PaymentForm();
        payment.setPaymentDate(LocalDate.now());
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        return showDetail(sponsorService.findById(id), payment, model);
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
        } catch (InvalidSponsorAmountException ex) {
            bindingResult.rejectValue("sponsorshipAmount", "belowPaid", ex.getMessage());
            return showForm(form, id, model);
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSponsor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String name = sponsorService.findById(id).getName();
        sponsorService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Sponsor \"" + name + "\" deleted.");
        return "redirect:/sponsors";
    }

    @PostMapping("/{id}/payments")
    public String recordPayment(@PathVariable Long id,
                                @Valid @ModelAttribute("payment") PaymentForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Sponsor sponsor = sponsorService.findById(id);
        if (bindingResult.hasErrors()) {
            return showDetail(sponsor, form, model);
        }
        try {
            paymentService.record(id, form);
        } catch (PaymentNotAllowedException ex) {
            bindingResult.rejectValue(ex.getField(), "notAllowed", ex.getMessage());
            return showDetail(sponsor, form, model);
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Payment of " + Money.format(form.getAmount()) + " recorded for " + sponsor.getName() + ".");
        return "redirect:/sponsors/" + id;
    }

    @PostMapping("/{id}/payments/{paymentId}/delete")
    public String deletePayment(@PathVariable Long id, @PathVariable Long paymentId,
                                RedirectAttributes redirectAttributes) {
        Sponsor sponsor = paymentService.delete(paymentId);
        redirectAttributes.addFlashAttribute("successMessage", "Payment removed from " + sponsor.getName() + ".");
        return "redirect:/sponsors/" + sponsor.getId();
    }

    private String showDetail(Sponsor sponsor, PaymentForm paymentForm, Model model) {
        model.addAttribute("sponsor", sponsor);
        model.addAttribute("payment", paymentForm);
        model.addAttribute("payments", paymentService.findBySponsor(sponsor.getId()));
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("activity", activityService.forSubject(Alert.SPONSOR, sponsor.getId()));
        model.addAttribute("today", LocalDate.now());
        return DETAIL_VIEW;
    }

    private String showForm(SponsorForm form, Long sponsorId, Model model) {
        model.addAttribute("sponsor", form);
        model.addAttribute("sponsorId", sponsorId);
        model.addAttribute("statuses", SponsorshipStatus.values());
        model.addAttribute("tiers", SponsorshipTier.values());
        model.addAttribute("events", eventService.findAll());
        return FORM_VIEW;
    }
}
