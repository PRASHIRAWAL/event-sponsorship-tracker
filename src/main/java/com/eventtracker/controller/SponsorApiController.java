package com.eventtracker.controller;

import com.eventtracker.model.PaymentForm;
import com.eventtracker.model.PaymentResponse;
import com.eventtracker.model.PaymentStatus;
import com.eventtracker.model.SponsorFilter;
import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorResponse;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import com.eventtracker.service.PaymentService;
import com.eventtracker.service.SponsorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SponsorApiController {

    private final SponsorService sponsorService;
    private final PaymentService paymentService;

    public SponsorApiController(SponsorService sponsorService, PaymentService paymentService) {
        this.sponsorService = sponsorService;
        this.paymentService = paymentService;
    }

    @GetMapping("/sponsors")
    public List<SponsorResponse> getSponsors(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) SponsorshipStatus status,
                                             @RequestParam(required = false) Long eventId,
                                             @RequestParam(required = false) SponsorshipTier tier,
                                             @RequestParam(required = false) PaymentStatus paymentStatus) {
        return sponsorService.search(new SponsorFilter(q, status, eventId, tier, paymentStatus)).stream()
                .map(SponsorResponse::from)
                .toList();
    }

    @GetMapping("/sponsors/{id}")
    public SponsorResponse getSponsor(@PathVariable Long id) {
        return SponsorResponse.from(sponsorService.findById(id));
    }

    @PostMapping("/sponsors")
    public ResponseEntity<SponsorResponse> createSponsor(@Valid @RequestBody SponsorForm form) {
        SponsorResponse created = SponsorResponse.from(sponsorService.create(form));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/sponsors/{id}")
    public SponsorResponse updateSponsor(@PathVariable Long id, @Valid @RequestBody SponsorForm form) {
        return SponsorResponse.from(sponsorService.update(id, form));
    }

    /** Deletes the sponsor and its payment history. */
    @DeleteMapping("/sponsors/{id}")
    public ResponseEntity<Void> deleteSponsor(@PathVariable Long id) {
        sponsorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sponsors/{id}/payments")
    public List<PaymentResponse> getPayments(@PathVariable Long id) {
        sponsorService.findById(id);
        return paymentService.findBySponsor(id).stream().map(PaymentResponse::from).toList();
    }

    @PostMapping("/sponsors/{id}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@PathVariable Long id, @Valid @RequestBody PaymentForm form) {
        PaymentResponse created = PaymentResponse.from(paymentService.record(id, form));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/payments/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/payments/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.delete(paymentId);
        return ResponseEntity.noContent().build();
    }
}
