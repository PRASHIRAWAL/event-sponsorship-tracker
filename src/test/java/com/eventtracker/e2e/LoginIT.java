package com.eventtracker.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.assertj.core.api.Assertions.assertThat;

/** Sign-in, sign-out and the read-only viewer role in a real browser. */
class LoginIT extends BaseE2ETest {

    @Override
    protected boolean signInAsAdmin() {
        return false;
    }

    @Test
    void pagesRequireSignIn() {
        open("/sponsors");

        wait.until(ExpectedConditions.urlContains("/login"));
        assertThat(waitFor(By.id("login-form")).isDisplayed()).isTrue();
    }

    @Test
    void wrongPasswordShowsAnError() {
        submitLogin(ADMIN, "not-the-password");

        assertThat(waitFor(By.id("login-error")).getText()).contains("Incorrect user name or password");
    }

    @Test
    void adminSignsInAndOut() {
        signIn(ADMIN, ADMIN_PASSWORD);
        assertThat(pageTitle()).isEqualTo("Dashboard");
        assertThat(driver.findElement(By.id("current-user")).getText()).isEqualTo(ADMIN);

        waitFor(By.cssSelector("#user-menu summary")).click();
        waitFor(By.id("logout-button")).click();

        assertThat(waitFor(By.id("logout-message")).getText()).contains("signed out");
        open("/dashboard");
        wait.until(ExpectedConditions.urlContains("/login"));
    }

    @Test
    void viewerHasReadOnlyAccess() {
        signIn(VIEWER, VIEWER_PASSWORD);
        assertThat(driver.findElement(By.id("current-role")).getText()).isEqualTo("Viewer");

        open("/events");
        assertThat(pageTitle()).isEqualTo("Events");
        assertThat(driver.findElements(By.id("create-event"))).isEmpty();
        assertThat(driver.findElements(By.id("quick-add"))).isEmpty();
        assertThat(driver.findElements(By.cssSelector("nav.nav a[href='/alerts']"))).isEmpty();
        assertThat(driver.findElements(By.cssSelector("nav.nav a[href='/activity']"))).isEmpty();

        open("/events/new");
        assertThat(waitFor(By.id("access-denied")).getText()).contains("read-only access");
    }
}
