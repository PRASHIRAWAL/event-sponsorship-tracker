package com.eventtracker.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationIT extends BaseE2ETest {

    @Test
    void dashboardLoadsWithSummaryCards() {
        open("/dashboard");

        assertThat(pageTitle()).isEqualTo("Dashboard");
        waitFor(By.id("summary-cards"));
        for (String id : new String[] {"total-events", "upcoming-events", "past-events", "total-sponsors",
                "committed-amount", "open-amount", "alert-count"}) {
            assertThat(driver.findElement(By.id(id)).getText()).as("KPI card %s", id).isNotBlank();
        }
    }

    @Test
    void headerLinksOpenEachSection() {
        open("/dashboard");

        clickNav("/events");
        assertThat(pageTitle()).isEqualTo("Events");

        clickNav("/sponsors");
        assertThat(pageTitle()).isEqualTo("Sponsors");

        clickNav("/alerts");
        assertThat(pageTitle()).startsWith("Alerts & Exceptions");

        clickNav("/dashboard");
        assertThat(pageTitle()).isEqualTo("Dashboard");
    }

    private void clickNav(String path) {
        waitFor(By.cssSelector("nav.nav a[href='" + path + "']")).click();
        wait.until(ExpectedConditions.urlMatches(path + "$"));
    }
}
