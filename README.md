# Containerized Event Sponsorship Tracker

## Project Overview
The **Containerized Event Sponsorship Tracker** is a comprehensive DevOps-driven web application designed to track events, sponsors, sponsorship packages, payment statuses, and deliverables. It streamlines the end-to-end sponsorship lifecycle for collegiate and enterprise events with automated CI/CD pipelines, containerization, and configuration management.

---

## Project Status
**Current milestone:** v2 – payments, funding targets, tiers, reports, activity log, delete/export, pagination and a redesigned UI, on top of the Task 6 MVP and the Jenkins/Docker/Ansible pipeline. See the [project backlog](docs/BACKLOG.md) for completed and remaining work.

---

## Student Details
- **Student Name:** Prashi Rawal
- **Roll Number:** 23102C0067
- **Subject:** DevOps
- **Academic Year:** 2026-2027

---

## Technology Stack
- **Backend:** Java 21, Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Actuator)
- **Frontend:** Thymeleaf, HTML5, CSS3 (light/dark themes, CSS-only charts), vanilla JavaScript
- **Database:** MySQL 8.x (H2 in-memory for tests and the `dev` profile)
- **Build & Dependency Management:** Apache Maven
- **Version Control:** Git, GitHub
- **Continuous Integration / Continuous Deployment:** Jenkins
- **Automated Testing:** Selenium WebDriver, JUnit 5
- **Containerization & Orchestration:** Docker, Docker Compose
- **Reverse Proxy / Web Server:** Nginx
- **Configuration Management:** Ansible

---

## High-Level Architecture
```text
User / Browser
      ↓
    Nginx (Port 80 - Reverse Proxy)
      ↓
Spring Boot Application (Port 8080)
      ↓
 MySQL Database (Port 3306)
```

---

## Approved MVP Features (15 Functions)
1. **Create Event:** Register new events with title, dates, venue, and sponsorship target.
2. **Create Sponsor:** Maintain sponsor organization profiles and contact persons.
3. **Create Sponsorship Record:** Link sponsors with designated events.
4. **Record Sponsorship Amount:** Log committed contribution sums and currency tiers.
5. **Track Sponsorship Status:** Monitor lifecycle states (Proposed, Contract Sent, Signed, Rejected).
6. **Track Payment Status:** Monitor financial clearance (Pending, Partial, Completed).
7. **Update Sponsorship:** Modify terms, tier allocations, or remarks.
8. **Search Events:** Dynamic query mechanism to locate events by name or status.
9. **Search Sponsors:** Query sponsors by industry, tier, or representative.
10. **Dashboard Summary:** Aggregated metrics for total revenue, active sponsors, and event counts.
11. **Sponsorship List:** Paginated and sortable master view of all sponsorship deals.
12. **Status Drill-down:** Filterable view isolating sponsorships by operational state.
13. **Exception View:** Flag anomalous states such as overdue payments or unsigned contracts.
14. **Input Validation:** Robust server-side and client-side validation rules.
15. **Application Health Check:** Actuator-driven monitoring endpoint for liveness and readiness.

### v2 Additions
- **Payments:** record payments (amount, date, method, reference) per sponsor; amount paid, outstanding balance and a derived payment status (Unpaid, Partially paid, Paid, Overdue). Payments cannot exceed the outstanding balance, cannot be recorded for cancelled sponsorships, and a sponsor's amount cannot drop below what was already paid.
- **Funding targets:** optional sponsorship target per event with progress bars on the dashboard, event list and event page.
- **Sponsorship tiers & industry:** Platinum / Gold / Silver / Bronze / Community packages, industry field, payment due date and notes.
- **New alert rules:** *Payment overdue* (High) and *Event below sponsorship target* within 30 days (Medium); alerts are sorted by severity and can be filtered.
- **Delete:** events (sponsors are kept and unlinked) and sponsors (with their payments), behind a confirmation dialog; REST `DELETE` endpoints.
- **Pagination & sorting:** event and sponsor lists with sortable columns (whitelisted), 10/25/50 rows per page.
- **CSV export:** sponsors and event funding, respecting the current filters (formula-injection safe).
- **Reports page:** event funding table with totals, tier donut chart, payments by method, top sponsors.
- **Activity log:** audit trail of every create/update/delete/payment, shown on the dashboard, detail pages and `/activity`.
- **Redesigned UI:** top navigation bar with quick-add and account menus, light/dark theme toggle, responsive tables, dashboard charts, keyboard shortcut `/` for search.
- **Motion:** animated sign-in page (drifting colour background, live-looking preview cards, rotating headline, floating-label fields); in the app, a soft ambient background, staggered page reveals, count-up dashboard figures, filling progress bars and charts, hover lift on cards and a personal greeting. Animations switch off for users who prefer reduced motion.
- **Sign-in & roles (Spring Security):** sign-in page with "keep me signed in"; **Admin** can create, edit and delete and sees alerts, the activity log and individual payment records; **Viewer** gets a read-only view of the dashboard, events, sponsors (with payment status and progress) and reports (everything else is hidden and blocked with 403). Passwords are stored as BCrypt hashes, forms are CSRF-protected, an account is locked for 15 minutes after 5 wrong passwords, and the activity log records who made each change. The REST API accepts HTTP Basic.
- **Demo data & dev profile:** `APP_DEMO_DATA=true` seeds realistic sample data into an empty database.

---

## Project Structure
```text
event-sponsorship-tracker/
│
├── .github/
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       ├── feature_request.md
│       └── task.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── eventtracker/
│   │   │           ├── controller/
│   │   │           ├── service/
│   │   │           ├── repository/
│   │   │           ├── model/
│   │   │           └── EventSponsorshipTrackerApplication.java
│   │   │
│   │   └── resources/
│   │       ├── templates/
│   │       ├── static/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── eventtracker/
│                   └── EventSponsorshipTrackerApplicationTests.java
│
├── .gitignore
├── README.md
└── pom.xml
```

---

## Build, Test, and Execution Commands

### Prerequisites
- JDK 21 installed (`java -version` verifies Java 21)
- Apache Maven 3.9+ installed (`mvn -version`)

### 1. Build and Run Tests
```powershell
mvn clean test
```

### 2. Package the Application (JAR)
```powershell
mvn clean package
```

### 3. Run the Application Locally
```powershell
# With MySQL (localhost:3306, or set DB_URL / DB_USERNAME / DB_PASSWORD)
mvn spring-boot:run

# Without MySQL: in-memory H2 database pre-filled with demo data
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

#### Sign-in accounts
| Variable | Purpose | Default |
|---|---|---|
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Full access | `admin` / *random password printed in the log at startup* |
| `VIEWER_USERNAME` / `VIEWER_PASSWORD` | Read-only access | `viewer` / *disabled when no password is set* |
| `REMEMBER_ME_KEY` | Signs "keep me signed in" cookies | random (cookies end on restart) |

The `dev` profile uses **admin / admin123** and **viewer / viewer123** and shows them on the sign-in page.
Never use these outside local development.

### Run the full stack with Docker Compose (MySQL + app + Nginx)
```powershell
mvn -B package -DskipTests
copy .env.example .env      # then set the passwords in .env
docker compose up -d --build
# http://localhost:8088 (Nginx) and http://localhost:8089 (app)
```

### 4. Application Access URLs
- **Web Interface:** [http://localhost:8080](http://localhost:8080) (redirects to the sign-in page)
- **Health Check Endpoint:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) (public; details only when signed in)

### 5. Application Pages
| Page | URL |
|---|---|
| Dashboard (KPIs, funding progress, status/tier charts, alerts, recent activity) | `/dashboard` |
| Events (search, Upcoming/Past filter, sorting, paging, funding, CSV export) | `/events` |
| Event details (funding progress, linked sponsors, history, delete) | `/events/{id}` |
| Sponsors (search, status/tier/payment/event filters, sorting, paging, CSV export) | `/sponsors` |
| Sponsor details (payments, record payment, contact, notes, history, delete) | `/sponsors/{id}` |
| Reports (event funding, tiers, payment methods, top sponsors) | `/reports` |
| Alerts & exceptions (filter by severity) | `/alerts` |
| Activity log (who changed what) | `/activity` |
| Sign in | `/login` |

### 6. REST API
All endpoints need an account: send HTTP Basic credentials (e.g. `curl -u admin:password http://localhost:8080/api/events`).
`GET` works for every user; `POST`, `PUT` and `DELETE` need the admin account (otherwise 403). Missing or wrong credentials return 401.

| Method | Endpoint | Purpose |
|---|---|---|
| GET / POST | `/api/events` | List or create events |
| GET / PUT / DELETE | `/api/events/{id}` | Get, update or delete an event (sponsors are unlinked) |
| GET | `/api/events/{id}/funding` | Target, committed, pipeline, collected and outstanding for an event |
| GET / POST | `/api/sponsors` | List (optional `q`, `status`, `eventId`, `tier`, `paymentStatus` filters) or create sponsors |
| GET / PUT / DELETE | `/api/sponsors/{id}` | Get, update or delete a sponsor (with its payments) |
| GET / POST | `/api/sponsors/{id}/payments` | List or record payments (422 when a payment rule is broken) |
| DELETE | `/api/payments/{id}` | Remove a payment |
| GET | `/api/dashboard/summary` | Dashboard indicators (incl. collected, outstanding, overdue, tiers) |
| GET | `/api/dashboard/exceptions` | Current alerts, most severe first |
| GET | `/api/reports/events`, `/api/reports/tiers`, `/api/reports/payment-methods` | Reporting data |
| GET | `/api/activity?limit=20` | Recent activity (max 100) |
| GET | `/events/export.csv`, `/sponsors/export.csv` | CSV downloads (accept the list filters) |

---

## DevOps Pipeline Workflow
1. **Local Development:** Feature branch development with unit and integration tests.
2. **Source Control:** Git branching model with pull requests targeting `development`.
3. **Continuous Integration (Jenkins):** Automated triggers on push to compile, test, and build JAR. Pipeline defined in [`Jenkinsfile`](Jenkinsfile); setup steps in [`docs/JENKINS_SETUP.md`](docs/JENKINS_SETUP.md).
4. **Automated Testing:** Selenium end-to-end tests verifying UI flows and validations (`mvn -B -Pe2e verify`; see [`docs/SELENIUM_E2E.md`](docs/SELENIUM_E2E.md)).
5. **Containerization:** Docker container image generation for the Spring Boot app and Nginx (deployed by the Jenkins pipeline; see [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)).
6. **Configuration & Deployment:** Ansible playbooks orchestrating multi-container deployment.

---

## Branching Strategy & Naming Policy

The repository adheres to a strict Git branching policy:

| Branch Type | Branch Name / Pattern | Purpose |
|---|---|---|
| **Production Branch** | `main` | Production-ready, stable codebase. Only merged via tested pull requests. |
| **Development Branch** | `development` | Integration branch for ongoing sprint deliverables and features. |
| **Feature Branches** | `feature/<feature-name>` | Dedicated branches for MVP feature implementation. |
| **Bug Fix Branches** | `bugfix/<bug-name>` | Non-critical defect corrections. |
| **Hotfix Branches** | `hotfix/<issue-name>` | Critical patches applied directly to production. |

### Feature Branch Examples:
- `feature/event-management`
- `feature/sponsor-management`
- `feature/sponsorship-management`
- `feature/dashboard-summary`
- `feature/exception-view`

