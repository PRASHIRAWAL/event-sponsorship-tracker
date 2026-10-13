# Project Backlog – Event Sponsorship Tracker

Status of the 15 approved MVP features (see README) after Task 6, plus the remaining DevOps work.

Legend: ✅ Done · 🟡 Partly done · ⬜ Not started

## MVP Features

| # | Feature | Status | Notes |
|---|---|---|---|
| 1 | Create Event | ✅ | Task 5. Name, date, venue, description (Task 3 data model). A sponsorship target per event is not stored yet. |
| 2 | Create Sponsor | ✅ | Task 6. Name, contact person, email, phone, amount, status, optional event link. |
| 3 | Create Sponsorship Record | 🟡 | A sponsor record links to **one** event. A separate Sponsorship entity (one sponsor across many events, as in the Task 3 data model) is still to do. |
| 4 | Record Sponsorship Amount | ✅ | Decimal amount (12 digits, 2 decimals). Currency tiers are not modelled. |
| 5 | Track Sponsorship Status | ✅ | Proposed, Pending, Confirmed, Completed, Cancelled (Task 3 values). |
| 6 | Track Payment Status | ⬜ | Not started (Pending/Partial/Paid/Overdue in Task 3). |
| 7 | Update Sponsorship | ✅ | Edit sponsor page and `PUT /api/sponsors/{id}`. Tier allocations and remarks are not modelled. |
| 8 | Search Events | ✅ | Keyword (name or venue) plus Upcoming/Past filter. |
| 9 | Search Sponsors | ✅ | Keyword (name, contact person or email) plus status and event filters. Industry/tier fields do not exist yet. |
| 10 | Dashboard Summary | ✅ | Totals for events, sponsors, committed and open amounts, and open alerts. |
| 11 | Sponsorship List | 🟡 | Filterable list is done; pagination and column sorting are still to do. |
| 12 | Status Drill-down | ✅ | Dashboard status counts link to filtered sponsor lists; event cards link to Upcoming/Past lists; event detail page lists its sponsors. |
| 13 | Exception View | ✅ | `/alerts` page and `/api/dashboard/exceptions` (rules below). Overdue-payment alerts need feature 6. |
| 14 | Input Validation | ✅ | Server-side Bean Validation with field messages, plus HTML5 client-side checks. |
| 15 | Application Health Check | ✅ | Spring Boot Actuator `/actuator/health` including the database. |

## Remaining Work (Next Iterations)

| Priority | Item |
|---|---|
| High | Payment status tracking and overdue-payment alert (feature 6) |
| High | Separate Sponsorship entity so one sponsor can support several events (feature 3) |
| High | Selenium end-to-end tests for the main UI flows |
| High | Jenkins pipeline: build, test and package on every push |
| High | Dockerfile and Docker Compose (application + MySQL), Nginx reverse proxy |
| Medium | Ansible playbook for deployment |
| Medium | Pagination and sorting on the event and sponsor lists (feature 11) |
| Medium | Delete endpoints for events and sponsors |
| Low | Event sponsorship target, sponsor industry/tier fields |
| Low | Authentication and user roles |

## Alert Rules (Implemented)

Alerts are calculated when the page or API is requested; nothing extra is stored in the database.
Sponsors with status **Cancelled** are excluded from all sponsor checks.

| Rule | Raised when |
|---|---|
| Missing contact person | A sponsor has no contact person. |
| No email or phone | A sponsor has neither an email address nor a phone number. |
| Not linked to an event | A sponsor is not linked to any event. |
| Committed sponsorship without amount | Status is Confirmed or Completed but no amount is recorded. |
| Undecided sponsorship for a past event | Status is Proposed or Pending and the linked event date has passed. |
| Upcoming event without sponsors | An event dated today or later has no sponsors, or only cancelled ones. |

**Event status** is derived from the event date: today or later = Upcoming, earlier = Past.
**Committed amount** = Confirmed + Completed; **open pipeline** = Proposed + Pending. Sponsors without an amount are not included in totals, and the dashboard shows how many there are.

## Release-Ready Baseline

Suggested tag: **`v0.1.0-mvp`** — the first working baseline of the MVP. It uses a 0.x version because
features 3, 6 and 11 and the CI/CD and container work are still open.

### How to verify the baseline

1. Check out the tagged commit: `git checkout v0.1.0-mvp`
2. Run the automated tests: `mvn clean test` → expect `BUILD SUCCESS` and 0 failures. Tests use an
   in-memory H2 database, so MySQL is not needed for this step.
3. Start MySQL: `docker start eventtracker-mysql`
4. Run the application: `mvn spring-boot:run`
5. Check health: `http://localhost:8080/actuator/health` → `"status":"UP"` with `db` UP.
6. Smoke-test the pages: `/dashboard`, `/events`, `/sponsors`, `/alerts`. Create an event and a sponsor,
   edit both, try invalid input, and confirm the dashboard totals and alerts change as expected.
7. Smoke-test the API: `GET /api/events`, `GET /api/sponsors`, `GET /api/dashboard/summary`.
