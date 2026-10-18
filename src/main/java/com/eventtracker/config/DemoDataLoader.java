package com.eventtracker.config;

import com.eventtracker.model.Event;
import com.eventtracker.model.PaymentForm;
import com.eventtracker.model.PaymentMethod;
import com.eventtracker.model.Sponsor;
import com.eventtracker.model.SponsorForm;
import com.eventtracker.model.SponsorshipStatus;
import com.eventtracker.model.SponsorshipTier;
import com.eventtracker.repository.EventRepository;
import com.eventtracker.service.EventService;
import com.eventtracker.service.PaymentService;
import com.eventtracker.service.SponsorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds a realistic set of events, sponsors and payments so a fresh installation has something to show.
 * Enabled with app.demo-data=true (env APP_DEMO_DATA=true) and only runs when there are no events yet,
 * so it never touches an existing database. Dates are relative to today, so the data stays current.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final SponsorService sponsorService;
    private final PaymentService paymentService;

    public DemoDataLoader(EventRepository eventRepository, EventService eventService,
                          SponsorService sponsorService, PaymentService paymentService) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.sponsorService = sponsorService;
        this.paymentService = paymentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (eventRepository.count() > 0) {
            log.info("Demo data skipped: the database already contains events");
            return;
        }
        LocalDate today = LocalDate.now();

        Event techFest = event("TechNova 2026 – Annual Tech Fest", today.plusDays(18), "Main Auditorium",
                "Three-day festival with hackathons, robotics and startup expo.", "500000");
        Event hackathon = event("CodeSprint 24h Hackathon", today.plusDays(45), "Innovation Lab, Block C",
                "Overnight hackathon for 300 students across 60 teams.", "250000");
        Event cultural = event("Rang Utsav Cultural Night", today.plusDays(9), "Open Air Theatre",
                "Music, dance and drama performances by all departments.", "200000");
        Event summit = event("Women in Tech Leadership Summit", today.plusDays(70), "Convention Hall",
                "Panel talks and mentoring sessions with industry leaders.", "300000");
        Event sports = event("Inter-College Sports Meet", today.minusDays(25), "University Sports Complex",
                "Athletics, football and basketball tournaments.", "150000");
        Event alumni = event("Alumni Homecoming", today.minusDays(60), "Seminar Hall",
                "Networking evening for graduates of the last decade.", null);

        Sponsor infosys = sponsor("Infosys Foundation", "Information Technology", "Ananya Rao",
                "ananya.rao@infosys.example", "+91 98450 11223", "200000", SponsorshipStatus.CONFIRMED,
                SponsorshipTier.PLATINUM, today.plusDays(10), techFest, "Title sponsor; logo on all banners.");
        Sponsor redbull = sponsor("Red Bull India", "Beverages", "Karan Mehta", "karan.mehta@redbull.example",
                "+91 99000 45678", "75000", SponsorshipStatus.CONFIRMED, SponsorshipTier.GOLD,
                today.minusDays(5), techFest, "Energy drink stalls in the expo area.");
        Sponsor github = sponsor("GitHub Education", "Software", "Priya Nair", "priya@github.example", null,
                "120000", SponsorshipStatus.COMPLETED, SponsorshipTier.PLATINUM, today.minusDays(2), hackathon,
                "Prizes for top three teams plus Copilot licences.");
        Sponsor devfolio = sponsor("Devfolio", "Software", "Rahul Verma", "rahul@devfolio.example",
                "+91 80 4000 1234", "50000", SponsorshipStatus.PENDING, SponsorshipTier.SILVER,
                today.plusDays(30), hackathon, "Waiting for budget approval.");
        Sponsor amul = sponsor("Amul Dairy", "Food & Beverages", "Meera Patel", "meera.patel@amul.example",
                "+91 79 2658 9000", "60000", SponsorshipStatus.CONFIRMED, SponsorshipTier.GOLD,
                today.plusDays(5), cultural, null);
        Sponsor bookmyshow = sponsor("BookMyShow", "Entertainment", null, "events@bookmyshow.example", null,
                "40000", SponsorshipStatus.PROPOSED, SponsorshipTier.SILVER, null, cultural,
                "Ticketing partner proposal sent.");
        Sponsor deloitte = sponsor("Deloitte", "Consulting", "Sneha Iyer", "sneha.iyer@deloitte.example",
                "+91 22 6185 4000", "150000", SponsorshipStatus.PROPOSED, SponsorshipTier.PLATINUM, null, summit,
                null);
        Sponsor decathlon = sponsor("Decathlon Sports", "Retail", "Arjun Singh", "arjun@decathlon.example",
                "+91 98111 22334", "90000", SponsorshipStatus.COMPLETED, SponsorshipTier.GOLD,
                today.minusDays(40), sports, "Kits and equipment for all teams.");
        sponsor("Local Print Hub", "Printing", "Vikram Joshi", null, "+91 90000 12345", "15000",
                SponsorshipStatus.PENDING, SponsorshipTier.COMMUNITY, null, sports, null);
        sponsor("Zomato", "Food Delivery", "Neha Kapoor", "neha@zomato.example", null, null,
                SponsorshipStatus.CONFIRMED, SponsorshipTier.BRONZE, null, cultural, "Amount to be confirmed.");
        sponsor("Campus Café", "Food & Beverages", null, null, null, "10000", SponsorshipStatus.PROPOSED,
                SponsorshipTier.COMMUNITY, null, null, null);
        sponsor("Old Mill Traders", "Retail", "Suresh Kumar", "suresh@oldmill.example", null, "25000",
                SponsorshipStatus.CANCELLED, SponsorshipTier.BRONZE, null, alumni, "Withdrew in August.");

        pay(infosys, "100000", today.minusDays(12), PaymentMethod.BANK_TRANSFER, "NEFT-88213");
        pay(infosys, "50000", today.minusDays(3), PaymentMethod.BANK_TRANSFER, "NEFT-90122");
        pay(redbull, "25000", today.minusDays(20), PaymentMethod.UPI, "UPI-RB-5521");
        pay(github, "120000", today.minusDays(8), PaymentMethod.CARD, "INV-GH-2026-04");
        pay(amul, "30000", today.minusDays(1), PaymentMethod.CHEQUE, "CHQ-004512");
        pay(decathlon, "90000", today.minusDays(35), PaymentMethod.BANK_TRANSFER, "RTGS-77120");
        log.info("Demo data loaded: {} events, {} sponsors", eventRepository.count(), sponsorService.findAll().size());
    }

    private Event event(String name, LocalDate date, String venue, String description, String target) {
        Event event = new Event(name, date, venue, description);
        event.setSponsorshipTarget(target == null ? null : new BigDecimal(target));
        return eventService.create(event);
    }

    private Sponsor sponsor(String name, String industry, String contact, String email, String phone, String amount,
                            SponsorshipStatus status, SponsorshipTier tier, LocalDate due, Event event, String notes) {
        SponsorForm form = new SponsorForm();
        form.setName(name);
        form.setIndustry(industry);
        form.setContactPerson(contact);
        form.setEmail(email);
        form.setPhone(phone);
        form.setSponsorshipAmount(amount == null ? null : new BigDecimal(amount));
        form.setSponsorshipStatus(status);
        form.setTier(tier);
        form.setPaymentDueDate(due);
        form.setEventId(event == null ? null : event.getId());
        form.setNotes(notes);
        return sponsorService.create(form);
    }

    private void pay(Sponsor sponsor, String amount, LocalDate date, PaymentMethod method, String reference) {
        PaymentForm form = new PaymentForm();
        form.setAmount(new BigDecimal(amount));
        form.setPaymentDate(date);
        form.setMethod(method);
        form.setReference(reference);
        paymentService.record(sponsor.getId(), form);
    }
}
