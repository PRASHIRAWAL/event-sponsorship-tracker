# Containerized Event Sponsorship Tracker

## Project Overview
The **Containerized Event Sponsorship Tracker** is a comprehensive DevOps-driven web application designed to track events, sponsors, sponsorship packages, payment statuses, and deliverables. It streamlines the end-to-end sponsorship lifecycle for collegiate and enterprise events with automated CI/CD pipelines, containerization, and configuration management.

---

## Project Status
**Current milestone:** Task 6 – MVP baseline (event management, sponsor management, searchable dashboard, status drill-down and alerts). See the [project backlog](docs/BACKLOG.md) for completed and remaining work.

---

## Student Details
- **Student Name:** Prashi Rawal
- **Roll Number:** 23102C0067
- **Subject:** DevOps
- **Academic Year:** 2026-2027

---

## Technology Stack
- **Backend:** Java 21, Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Actuator)
- **Frontend:** Thymeleaf, HTML5, CSS3, JavaScript
- **Database:** MySQL 8.x
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
mvn spring-boot:run
```

### 4. Application Access URLs
- **Web Interface:** [http://localhost:8080](http://localhost:8080)
- **Health Check Endpoint:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### 5. Application Pages
| Page | URL |
|---|---|
| Dashboard (summary, search, status drill-down) | `/dashboard` |
| Events (list, search, filter by Upcoming/Past) | `/events` |
| Event details with linked sponsors | `/events/{id}` |
| Sponsors (list, search, filter by status/event) | `/sponsors` |
| Alerts & exceptions | `/alerts` |

### 6. REST API
| Method | Endpoint | Purpose |
|---|---|---|
| GET / POST | `/api/events` | List or create events |
| GET / PUT | `/api/events/{id}` | Get or update an event |
| GET / POST | `/api/sponsors` | List (optional `q`, `status`, `eventId` filters) or create sponsors |
| GET / PUT | `/api/sponsors/{id}` | Get or update a sponsor |
| GET | `/api/dashboard/summary` | Dashboard indicators |
| GET | `/api/dashboard/exceptions` | Current alerts |

---

## DevOps Pipeline Workflow
1. **Local Development:** Feature branch development with unit and integration tests.
2. **Source Control:** Git branching model with pull requests targeting `development`.
3. **Continuous Integration (Jenkins):** Automated triggers on push to compile, test, and build JAR.
4. **Automated Testing:** Selenium end-to-end tests verifying UI flows and validations.
5. **Containerization:** Docker container image generation for the Spring Boot app and Nginx.
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
