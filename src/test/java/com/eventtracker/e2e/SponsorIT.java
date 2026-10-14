package com.eventtracker.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SponsorIT extends BaseE2ETest {

    @Test
    void createSponsorLinkedToEvent() {
        String eventName = uniqueName("E2E Cultural Night");
        String sponsorName = uniqueName("E2E Acme Corp");
        createEvent(eventName, "Open Air Theatre", LocalDate.now().plusMonths(2));

        // Open the event and use its "Add Sponsor" button, which pre-selects the event.
        waitFor(By.linkText(eventName)).click();
        waitFor(By.id("add-sponsor")).click();
        assertThat(pageTitle()).isEqualTo("Add Sponsor");

        type(By.id("name"), sponsorName);
        type(By.id("contactPerson"), "Jane Doe");
        type(By.id("email"), "jane@acme.example");
        type(By.id("phone"), "+91 98765 43210");
        type(By.id("sponsorshipAmount"), "50000");
        driver.findElement(By.id("save-sponsor")).click();

        assertThat(waitFor(By.id("success-message")).getText())
                .contains("Sponsor \"" + sponsorName + "\" created successfully.");
        assertThat(driver.findElement(By.id("sponsors-table")).getText()).contains(sponsorName, eventName);

        open("/events");
        waitFor(By.linkText(eventName)).click();
        assertThat(waitFor(By.id("event-sponsors-table")).getText()).contains(sponsorName, "Jane Doe");
    }

    @Test
    void invalidPhoneShowsValidationError() {
        open("/sponsors/new");

        type(By.id("name"), uniqueName("E2E Bad Phone"));
        type(By.id("phone"), "abc");
        driver.findElement(By.id("save-sponsor")).click();

        waitFor(By.id("error-summary"));
        assertThat(pageTitle()).isEqualTo("Add Sponsor");
        assertThat(driver.findElement(By.id("phone")).getAttribute("class")).contains("invalid");
        assertThat(driver.findElements(By.cssSelector("#sponsor-form .field-error")))
                .extracting(WebElement::getText)
                .containsExactly("Enter a valid phone number (7-20 digits, spaces, +, - or brackets)");
    }

    @Test
    void sponsorWithoutContactPersonAppearsOnAlertsPage() {
        String sponsorName = uniqueName("E2E No Contact Ltd");

        open("/sponsors/new");
        type(By.id("name"), sponsorName);
        type(By.id("email"), "info@nocontact.example");
        driver.findElement(By.id("save-sponsor")).click();
        waitFor(By.id("success-message"));

        open("/alerts");
        List<WebElement> cards = waitFor(By.id("alerts-list")).findElements(By.cssSelector("li.alert-card"));
        List<String> titlesForSponsor = cards.stream()
                .filter(card -> card.findElement(By.cssSelector(".alert-card-subject strong")).getText().equals(sponsorName))
                .map(card -> card.findElement(By.cssSelector(".alert-card-title")).getText())
                .toList();

        assertThat(titlesForSponsor).anyMatch(title -> title.contains("Missing contact person"));
    }
}
