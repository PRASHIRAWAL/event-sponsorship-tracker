# Jenkins CI Setup

The `Jenkinsfile` at the repository root defines a declarative pipeline with these stages:

| Stage | Command | Result |
|---|---|---|
| Checkout | `checkout scm` | Pulls the configured branch from GitHub |
| Build | `mvn -B -V clean compile` | Compiles the Spring Boot application |
| Test | `mvn -B test` | Runs all unit and integration tests; JUnit report published |
| E2E Tests | `mvn -B -Pe2e verify` | Runs Selenium browser tests in headless Chrome (see `SELENIUM_E2E.md`) |
| Package | `mvn -B package -DskipTests` | Builds the JAR and archives it as a build artifact |
| Docker Image | `docker build` (app + Nginx) | Images tagged with the build number; only when `DEPLOY` is ticked |
| Deploy | `docker run` | Replaces `eventtracker-app` (port `APP_PORT`, default 8081) and `eventtracker-nginx` (port `NGINX_PORT`, default 80) |
| Verify Deployment | `curl` health checks | Requires `/actuator/health` = `UP` directly and through Nginx; archives `health-*.json` |

Deployment setup (MySQL user, `eventtracker-db` credential, rollback) is described in [`DEPLOYMENT.md`](DEPLOYMENT.md).

Tests run against the in-memory H2 database (`application-test.properties`, `test` profile), so the Jenkins agent does **not** need MySQL. The pipeline works on both Windows and Linux agents (`bat` / `sh` chosen automatically).

## 1. Required plugins

All are part of Jenkins' "Install suggested plugins" set. Verify under *Manage Jenkins → Plugins → Installed*:

- Pipeline
- Git
- JUnit
- Timestamper

## 2. Register JDK and Maven tools

*Manage Jenkins → Tools*:

| Section | Name (must match Jenkinsfile) | Value |
|---|---|---|
| JDK installations | `JDK21` | `JAVA_HOME` of a JDK 21 install, e.g. `C:\Program Files\Java\jdk-21` |
| Maven installations | `Maven3` | `MAVEN_HOME` of Maven 3.9+, or tick *Install automatically* (3.9.x) |

## 3. Create the pipeline job

1. *New Item* → name `event-sponsorship-tracker` → **Pipeline** → OK.
2. Under **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/PrashiRawal/event-sponsorship-tracker.git`
   - Branch Specifier: `*/development`
   - Script Path: `Jenkinsfile`
3. *(Optional)* Under **Build Triggers**, enable **Poll SCM** with `H/5 * * * *` to build automatically on new commits. (A GitHub webhook also works if Jenkins is reachable from the internet.)
4. Save → **Build Now**.

## 4. Verifying a build

- **Stage View** shows Checkout, Build, Test and Package all green.
- **Test Result** on the build page lists the JUnit test classes and their pass counts.
- **Build Artifacts** contains `event-sponsorship-tracker-0.0.1-SNAPSHOT.jar`.

## Running the same steps locally

```powershell
mvn -B clean compile
mvn -B test
mvn -B package -DskipTests
```
