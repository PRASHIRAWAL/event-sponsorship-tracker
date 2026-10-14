package com.eventtracker.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EventIT extends BaseE2ETest {

    @Test
    void createEventShowsSuccessAndListsIt() {
        String name = uniqueName("E2E Tech Fest");

        open("/events");
        waitFor(By.id("create-event")).click();
        assertThat(pageTitle()).isEqualTo("Create Event Defect");

        type(By.id("name"), name);
        setDate(By.id("eventDate"), LocalDate.now().plusMonths(2));
        type(By.id("venue"), "Main Auditorium");
        type(By.id("description"), "Created by Selenium");
        driver.findElement(By.id("save-event")).click();

        assertThat(waitFor(By.id("success-message")).getText())
                .contains("Event \"" + name + "\" created successfully.");
        // Tomcat may append ";jsessionid=..." to the first redirect of a new browser session; compare the path only.
        assertThat(URI.create(driver.getCurrentUrl()).getPath().replaceFirst(";.*$", "")).isEqualTo("/events");
        assertThat(driver.findElement(By.id("events-table")).getText()).contains(name, "Main Auditorium");
    }

    @Test
    void blankNameShowsValidationError() {
        open("/events/new");

        // Spaces pass the browser's "required" check but fail the server's @NotBlank rule.
        type(By.id("name"), "   ");
        setDate(By.id("eventDate"), LocalDate.now().plusMonths(1));
        type(By.id("venue"), "Seminar Hall");
        driver.findElement(By.id("save-event")).click();

        assertThat(waitFor(By.id("error-summary")).getText()).contains("Please correct the highlighted fields");
        assertThat(driver.findElement(By.cssSelector("#event-form .field-error")).getText())
                .isEqualTo("Event name is required");
        assertThat(pageTitle()).isEqualTo("Create Event");
    }

    @Test
    void searchFiltersEventsTable() {
        String token = uniqueName("Search");
        String matching = token + " Hackathon";
        String other = uniqueName("Other Workshop");
        createEvent(matching, "Lab 1", LocalDate.now().plusMonths(3));
        createEvent(other, "Lab 2", LocalDate.now().plusMonths(3));

        type(By.cssSelector("#event-search input[name='q']"), token);
        driver.findElement(By.cssSelector("#event-search button[type='submit']")).click();

        String table = waitFor(By.id("events-table")).getText();
        assertThat(table).contains(matching).doesNotContain(other);
    }
}
