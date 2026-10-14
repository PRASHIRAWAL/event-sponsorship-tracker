# Selenium End-to-End Tests

Browser tests live in `src/test/java/com/eventtracker/e2e/`. Each test class starts the full Spring Boot
application on a random port with the `test` profile (in-memory H2, **no MySQL needed**) and drives it
with Chrome through Selenium WebDriver. Selenium Manager downloads the matching ChromeDriver automatically.

| Class | Tests |
|---|---|
| `NavigationIT` | Dashboard shows all KPI cards; header links open Events, Sponsors, Alerts and Dashboard |
| `EventIT` | Create event (success message + table row); blank name shows validation error; search filters the table |
| `SponsorIT` | Create sponsor from an event's page (linked and shown on the event); invalid phone shows validation error; sponsor without contact person appears on the Alerts page |

Every test uses unique names, so tests do not depend on each other or on existing data.

## Running locally

Requires Google Chrome, JDK 21 and Maven 3.9+.

```powershell
mvn -B -Pe2e verify                    # E2E tests only, headless Chrome
mvn -B -Pe2e verify "-Dheadless=false" # watch the browser
mvn -B test                            # existing unit/integration tests (unchanged)
```

E2E classes are named `*IT`, so `mvn test` does not run them. Reports are written to
`target/failsafe-reports/`; a screenshot of the browser is saved to `target/e2e-screenshots/` when a test fails.

## Jenkins

The `E2E Tests` stage in the `Jenkinsfile` runs `mvn -B -Pe2e verify`, publishes the JUnit results and archives
any failure screenshots. The agent needs Google Chrome installed; tests run headless, so this works when Jenkins
runs as a Windows service.

The log may show `Unable to find CDP implementation matching <version>` warnings. These only concern Chrome
DevTools features, which the tests do not use, and can be ignored.
