package com.eventtracker.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for Selenium end-to-end tests.
 * Starts the full application on a random port with the H2 "test" profile (no MySQL needed)
 * and drives it with Chrome. Chrome runs headless unless -Dheadless=false is passed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseE2ETest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Path SCREENSHOT_DIR = Path.of("target", "e2e-screenshots");
    private static final AtomicInteger COUNTER = new AtomicInteger();
    protected static final String ADMIN = "admin";
    protected static final String ADMIN_PASSWORD = "admin-test-pass";
    protected static final String VIEWER = "viewer";
    protected static final String VIEWER_PASSWORD = "viewer-test-pass";

    @LocalServerPort
    private int port;

    protected WebDriver driver;
    protected WebDriverWait wait;

    // Runs before @AfterEach, so the browser is still open when a failure screenshot is taken.
    @RegisterExtension
    final AfterTestExecutionCallback screenshotOnFailure = context -> {
        if (context.getExecutionException().isPresent() && driver != null) {
            saveScreenshot(context.getRequiredTestClass().getSimpleName() + "-" + context.getRequiredTestMethod().getName());
        }
    };

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "true"))) {
            options.addArguments("--headless=new");
        }
        // Desktop width so the full navigation bar and table layout are shown.
        options.addArguments("--window-size=1440,1000", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, TIMEOUT);
        if (signInAsAdmin()) {
            signIn(ADMIN, ADMIN_PASSWORD);
        }
    }

    /** Tests start signed in as the admin; override to start signed out. */
    protected boolean signInAsAdmin() {
        return true;
    }

    /** Signs in through the sign-in page (accounts come from the "test" profile) and waits for the dashboard. */
    protected void signIn(String username, String password) {
        submitLogin(username, password);
        wait.until(ExpectedConditions.urlMatches("/dashboard$"));
    }

    protected void submitLogin(String username, String password) {
        open("/login");
        type(By.id("username"), username);
        type(By.id("password"), password);
        driver.findElement(By.id("login-button")).click();
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void open(String path) {
        driver.get("http://localhost:" + port + path);
    }

    protected WebElement waitFor(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected String pageTitle() {
        return waitFor(By.cssSelector("h1.page-title")).getText();
    }

    /** Returns a name that is unique per test run, so tests never depend on each other's data. */
    protected static String uniqueName(String prefix) {
        return prefix + " " + System.currentTimeMillis() + "-" + COUNTER.incrementAndGet();
    }

    protected void type(By locator, String text) {
        WebElement field = waitFor(locator);
        field.clear();
        field.sendKeys(text);
    }

    /** Sets a date input directly in ISO format, because typed dates depend on the browser locale. */
    protected void setDate(By locator, LocalDate date) {
        WebElement field = waitFor(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", field, date.toString());
    }

    /** Creates an event through the UI and waits for the events list with the success message. */
    protected void createEvent(String name, String venue, LocalDate date) {
        open("/events/new");
        type(By.id("name"), name);
        setDate(By.id("eventDate"), date);
        type(By.id("venue"), venue);
        driver.findElement(By.id("save-event")).click();
        waitFor(By.id("success-message"));
    }

    private void saveScreenshot(String name) {
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            Path source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(source, SCREENSHOT_DIR.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException ex) {
            System.err.println("Could not save screenshot for " + name + ": " + ex.getMessage());
        }
    }
}
