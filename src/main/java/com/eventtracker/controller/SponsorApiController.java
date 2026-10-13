package com.eventtracker.controller;

import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorResponse;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.service.SponsorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/sponsors")
public class SponsorApiController {

    private final SponsorService sponsorService;

    public SponsorApiController(SponsorService sponsorService) {
        this.sponsorService = sponsorService;
    }

    @GetMapping
    public List<SponsorResponse> getSponsors(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) SponsorshipStatus status,
                                             @RequestParam(required = false) Long eventId) {
        return sponsorService.search(q, status, eventId).stream()
                .map(SponsorResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SponsorResponse getSponsor(@PathVariable Long id) {
        return SponsorResponse.from(sponsorService.findById(id));
    }

    @PostMapping
    public ResponseEntity<SponsorResponse> createSponsor(@Valid @RequestBody SponsorForm form) {
        SponsorResponse created = SponsorResponse.from(sponsorService.create(form));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public SponsorResponse updateSponsor(@PathVariable Long id, @Valid @RequestBody SponsorForm form) {
        return SponsorResponse.from(sponsorService.update(id, form));
    }
}
