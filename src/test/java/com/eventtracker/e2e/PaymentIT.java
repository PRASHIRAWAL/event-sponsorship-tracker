package com.eventtracker.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentIT extends BaseE2ETest {

    @Test
    void recordPaymentThenDeleteSponsorThroughConfirmDialog() {
        String sponsorName = uniqueName("E2E Payment Co");

        open("/sponsors/new");
        type(By.id("name"), sponsorName);
        type(By.id("contactPerson"), "Ravi Kumar");
        type(By.id("email"), "ravi@payment.example");
        type(By.id("sponsorshipAmount"), "20000");
        driver.findElement(By.id("save-sponsor")).click();
        waitFor(By.id("success-message"));

        waitFor(By.linkText(sponsorName)).click();
        assertThat(waitFor(By.id("sponsor-payment-status")).getText()).isEqualTo("Unpaid");

        type(By.id("amount"), "5000");
        type(By.id("reference"), "UTR-E2E-1");
        driver.findElement(By.id("save-payment")).click();

        assertThat(waitFor(By.id("success-message")).getText())
                .contains("Payment of 5,000.00 recorded for " + sponsorName);
        assertThat(driver.findElement(By.id("sponsor-paid")).getText()).isEqualTo("5,000.00");
        assertThat(driver.findElement(By.id("sponsor-outstanding")).getText()).isEqualTo("15,000.00");
        assertThat(driver.findElement(By.id("sponsor-payment-status")).getText()).isEqualTo("Partially paid");
        assertThat(driver.findElement(By.id("payments-table")).getText()).contains("UTR-E2E-1");

        // Paying more than the outstanding balance is blocked in the browser (max attribute); the server
        // rule behind it is covered by PaymentAndLifecycleTest
        type(By.id("amount"), "15000.01");
        Object overflow = ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return document.getElementById('amount').validity.rangeOverflow;");
        assertThat(overflow).isEqualTo(Boolean.TRUE);

        // Delete asks for confirmation in a dialog before submitting
        driver.findElement(By.id("delete-sponsor")).click();
        assertThat(waitFor(By.id("confirm-dialog")).getText()).contains("Delete sponsor \"" + sponsorName + "\"?");
        WebElement previousMessage = driver.findElement(By.id("success-message"));
        driver.findElement(By.id("confirm-yes")).click();

        // Wait for the sponsor page (and its "payment recorded" message) to be replaced by the list
        wait.until(ExpectedConditions.stalenessOf(previousMessage));
        assertThat(waitFor(By.id("success-message")).getText()).contains("Sponsor \"" + sponsorName + "\" deleted.");
        assertThat(URI.create(driver.getCurrentUrl()).getPath().replaceFirst(";.*$", "")).isEqualTo("/sponsors");
    }

    @Test
    void eventTargetShowsFundingProgress() {
        String eventName = uniqueName("E2E Funded Fest");

        open("/events/new");
        type(By.id("name"), eventName);
        setDate(By.id("eventDate"), LocalDate.now().plusDays(12));
        type(By.id("venue"), "Main Hall");
        type(By.id("sponsorshipTarget"), "40000");
        driver.findElement(By.id("save-event")).click();
        waitFor(By.id("success-message"));

        waitFor(By.linkText(eventName)).click();
        waitFor(By.id("add-sponsor")).click();
        type(By.id("name"), uniqueName("E2E Gold Sponsor"));
        type(By.id("contactPerson"), "Asha");
        type(By.id("phone"), "+91 99887 76655");
        type(By.id("sponsorshipAmount"), "10000");
        driver.findElement(By.cssSelector("#sponsorshipStatus option[value='CONFIRMED']")).click();
        driver.findElement(By.id("save-sponsor")).click();
        waitFor(By.id("success-message"));

        open("/events");
        waitFor(By.linkText(eventName)).click();
        wait.until(ExpectedConditions.textToBe(By.id("event-committed-amount"), "10,000.00"));
        assertThat(driver.findElement(By.id("event-target")).getText()).isEqualTo("40,000.00");
        assertThat(driver.findElement(By.cssSelector(".funding-headline strong")).getText()).isEqualTo("25%");
    }
}
