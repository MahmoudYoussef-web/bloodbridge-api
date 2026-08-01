<div align="center">
<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=700&size=32&pause=1000&color=DC2626&center=true&vCenter=true&width=700&lines=BloodBridge+API;Geo-Matched+Blood+Requests+%7C+QR+Admission;Spring+Boot+3+%7C+MySQL+%7C+JWT+%7C+ML+Scoring" alt="Typing SVG" />

<br/>

**Every drop has a destination.**
A production-oriented REST API that matches blood donation requests to eligible donors in real time — progressive-radius broadcast, QR-verified admission, ML-assisted donor scoring, and full server-side security enforcement.

<br/>

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)

[![Architecture](https://img.shields.io/badge/Architecture-Layered_MVC-blue?style=flat-square)](#-system-architecture)
[![Security](https://img.shields.io/badge/Security-JWT_%2B_Ownership_Checks-orange?style=flat-square)](#-security)
[![Scoring](https://img.shields.io/badge/Donor_Scoring-FastAPI_%2B_Rule--Based_Fallback-purple?style=flat-square)](#-donor-scoring)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [System Architecture](#-system-architecture)
- [Blood Request Lifecycle](#-blood-request-lifecycle)
- [Features](#-features)
- [API Reference](#-api-reference)
- [Database Schema](#-database-schema)
- [Tech Stack](#-tech-stack)
- [Security](#-security)
- [Known Limitations](#-known-limitations)
- [Getting Started](#-getting-started)
- [Author](#-author)

---

## 🌐 Overview

**BloodBridge** solves a real coordination problem: matching an urgent blood request to donors who are both **nearby** and **actually eligible**, without flooding every donor in the city and without trusting the client to self-report eligibility.

### Design decisions that go beyond a typical CRUD API

| Challenge | How it's solved |
|---|---|
| A fixed search radius either misses donors or spams too many | Progressive radius expansion — starts at a base radius, expands in 5&nbsp;km steps up to 25&nbsp;km until the target donor count is reached (target scales with urgency: normal ×2.0, critical ×2.5, critical starts at 3× the base radius) |
| Donor eligibility can't be trusted client-side | Eligibility (chronic disease, recent surgery, active infection) is enforced **server-side at the interceptor level** — a request never reaches a controller if the donor is ineligible |
| Admission fraud (claiming a donation that didn't happen) | Every accepted response gets a single-use QR token (`SecureRandom`, 7-day expiry, organization-bound); admission is only confirmed by scanning it at the facility |
| ML scoring shouldn't be a single point of failure | Three-tier donor scoring: DB cache → FastAPI ML service (circuit breaker, 3-failure trip, 120s recovery) → rule-based fallback if the ML service is down or disabled |
| New donors have no history to score on | Cold-start scoring (neutral 0.5) with epsilon-greedy exploration (ε=0.20, weekly decay) so new donors still get a fair shot at being matched |
| Cross-organization data leakage | Every organization-scoped read/write validates resource ownership — an org can never read or act on another org's requests, responses, or QR data |
| Reliable event delivery without a message broker | Outbox pattern — domain events are persisted transactionally and re-published by a scheduled poller |

---

## 🏗️ System Architecture

```mermaid
graph TD
    Client([Frontend / Swagger UI]) -->|JWT Bearer| API[Spring Boot REST API :8080/api]

    subgraph Security Layer
        API --> JWT[JWT Auth Filter]
        JWT --> Blacklist[(Redis: Token Blacklist - optional)]
        JWT --> Interceptors
        Interceptors --> EmailGate[Email Verification Gate]
        Interceptors --> EligGate[Donor Eligibility Gate]
        Interceptors --> OrgGate[Org Approval Gate]
        Interceptors --> Controllers
    end

    subgraph Business Layer
        Controllers --> AuthService
        Controllers --> DonorServices[Donor / Profile / Eligibility]
        Controllers --> BroadcastService[Blood Request Broadcast]
        Controllers --> ActionService[Accept / QR / Complete]
        Controllers --> AdminService
    end

    subgraph Donor Scoring Pipeline
        BroadcastService --> Cache[(DB Score Cache)]
        Cache -.miss.-> FastAPI[FastAPI ML Service - optional]
        FastAPI -.circuit open / disabled.-> RuleBased[Rule-Based Fallback Scorer]
    end

    subgraph Reliability Layer
        ActionService -->|persist + outbox row| DB[(MySQL)]
        Outbox[Outbox Poller - 30s] --> DomainEvents[Domain Event Publisher]
    end

    subgraph Persistence
        AuthService --> DB
        DonorServices --> DB
        AdminService --> DB
    end
```

**Notes:**
- All security enforcement — JWT validation, email verification, donor eligibility, and organization approval — happens in the request pipeline **before** any controller logic runs.
- Redis is used for the optional JWT blacklist cache; rate limiting is in-memory (`InMemoryRateLimiter`), not Redis-backed.
- The FastAPI scoring service is an external, optional dependency — the platform functions fully without it via the rule-based fallback.

---

## 🔄 Blood Request Lifecycle

```mermaid
sequenceDiagram
    participant Org as Organization
    participant API as BloodBridge API
    participant Donor
    participant DB as MySQL

    Org->>API: POST /org/blood-requests (bloodType, urgency, radius)
    API->>API: Progressive radius expansion + eligibility filter + donor scoring
    API->>DB: Create PENDING responses for matched donors
    API-->>Org: 201 Created (BROADCASTED)

    Donor->>API: POST /donor/blood-requests/{id}/accept
    API->>API: Eligibility + one-active-response check
    API->>DB: Response → ACCEPTED, generate QR token (SecureRandom, 7d expiry)
    API-->>Donor: QR code (PNG)

    Note over Donor,Org: Donor travels to facility

    Org->>API: POST /org/scan-qr (rate-limited 30/min, org-bound)
    API->>API: Verify token + expiry + organization ownership
    API-->>Org: 200 OK (admission confirmed)

    Org->>API: POST /org/responses/{id}/complete
    API->>API: Ownership check + status validation
    API->>DB: Response → COMPLETED, +points, achievement check, eligibility recalc
    API->>DB: Persist DonationCompletedEvent (outbox)
    API-->>Org: 200 OK
```

<!-- ===== SCREENSHOT SLOT: QR admission example =====
     The strongest visual for this project. Run the accept flow against the local API,
     capture the returned QR PNG (GET the QR download endpoint), then replace the src below
     with your GitHub issue-attachment URL.
     Upload: new draft GitHub issue -> drag & drop the image -> copy the
     https://github.com/user-attachments/assets/... link -> paste here. -->
<p align="center">
  <img src="https://github.com/user-attachments/assets/REPLACE_ME_QR" width="240" alt="Admission QR code issued on donor acceptance"/>
</p>

---

## ✨ Features

### 🔐 Auth & Security
- JWT access + refresh tokens; BCrypt password hashing
- Role-based access control (`@PreAuthorize`) — Donor / Organization / Admin
- Server-side interceptor gates: email verification, donor eligibility, organization approval
- Ownership validation on all organization-scoped resources (cross-org access → 403)
- Idempotency-key support on mutating endpoints (safe client retries)
- Rate limiting on sensitive endpoints (QR scans, contact form)
- Optimistic locking (`@Version`) on `BloodRequest`, `RequestResponse`, `Donor`; pessimistic locking on donor acceptance to prevent double-accept races

### 🩸 Blood Request Matching
- Create → broadcast → accept/decline/ignore → QR admission → complete lifecycle
- Progressive radius expansion with urgency-scaled donor targets
- Unknown-blood-type fallback pool for non-critical requests
- Governorate-based fallback matching when coordinates are unavailable

### 🤖 Donor Scoring
- Optional FastAPI ML integration with a custom circuit breaker
- Rule-based fallback scorer (fully functional with ML disabled)
- Epsilon-greedy exploration so new/low-history donors still get matched

### 🏅 Gamification
- Points and levels awarded on donation completion
- Achievement/badge system (e.g. "First Donation") with criteria-based unlocking

### 🏢 Organization & Admin
- Organization profile, request management, response tracking with aggregate counts
- Admin console: organization approval workflow, platform-wide oversight, announcements, contact messages, settings

### 📊 Observability
- Micrometer metrics exported to Prometheus, Grafana dashboards
- Structured audit logging on critical operations
- springdoc OpenAPI / Swagger UI

---

## 📡 API Reference

41 endpoints across 5 controllers, all under the `/api` context-path · Full interactive docs at `/swagger-ui/index.html`

<!-- ===== SCREENSHOT SLOT: Swagger UI overview =====
     Screenshot http://localhost:8080/api/swagger-ui/index.html showing the 41 endpoints
     grouped under the 5 controllers. This is the single most valuable image for the README
     (instant proof that the API is documented). -->
<p align="center">
  <img src="https://github.com/user-attachments/assets/REPLACE_ME_SWAGGER" width="800" alt="Swagger UI - 41 endpoints across 5 controllers"/>
</p>

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register donor/organization (admin self-registration rejected) |
| `POST` | `/api/v1/auth/login` | Public | Login → access + refresh token |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate access token |
| `GET` | `/api/v1/donor/profile` | Donor | Donor profile + health data |
| `GET` | `/api/v1/donor/blood-requests` | Donor | Geo-matched active request feed |
| `POST` | `/api/v1/donor/blood-requests/{id}/accept` | Donor | Accept request → QR token issued |
| `GET` | `/api/v1/donor/achievements` | Donor | Earned/locked achievements, points, level |
| `POST` | `/api/v1/org/blood-requests` | Organization | Create + broadcast a blood request |
| `GET` | `/api/v1/org/blood-requests/{id}` | Organization | Request detail (ownership-enforced) |
| `POST` | `/api/v1/org/scan-qr` | Organization | Confirm admission (rate-limited 30/min) |
| `POST` | `/api/v1/org/responses/{id}/complete` | Organization | Complete a donation |
| `PUT` | `/api/v1/admin/organizations/{id}/approve` | Admin | Approve a pending organization |

<details>
<summary><b>📂 See all 41 endpoints (grouped by controller)</b></summary>

**AuthController** — register, login, refresh (3)
**DonorController** — profile, health profile update, request feed, accept/decline/ignore, QR download, achievements, eligibility status, donation history (10)
**OrganizationController** — profile, request CRUD/re-broadcast, response listing with aggregates, QR scan, complete (10)
**AdminController** — paginated users/donors/organizations/blood-requests/responses, organization approve/reject, achievements CRUD, contact messages, announcements, platform settings (17)
**PublicController** — contact form submission, public announcements (1)

</details>

<!-- ===== SCREENSHOT SLOT: real request/response samples =====
     Two high-value samples, matching the reference format:
       1. POST /api/v1/org/blood-requests — request body + 201 response (BROADCASTED)
       2. POST /api/v1/donor/blood-requests/{id}/accept — response showing the QR token/URL
     Capture from Swagger UI or Postman, upload, and replace the srcs below. -->
<details>
<summary><b>📸 Sample: create a blood request</b></summary>

```jsonc
// POST /api/v1/org/blood-requests
// Authorization: Bearer <org-token>

{
  "bloodType": "A_POSITIVE",
  "unitsNeeded": 2,
  "urgencyLevel": "CRITICAL",
  "searchRadiusKm": 10,
  "lat": 31.5017,
  "lng": 34.4668,
  "locationAddress": "Al-Shifa Hospital, Gaza City",
  "additionalNotes": "Critical need for pediatric ward"
}
```

```json
// 201 Created — BROADCASTED
{
  "id": 1,
  "bloodType": "A_POSITIVE",
  "unitsNeeded": 2,
  "urgencyLevel": "CRITICAL",
  "status": "BROADCASTED",
  "searchRadiusKm": 10,
  "additionalNotes": "Critical need for pediatric ward"
}
```

</details>

<details>
<summary><b>📸 Sample: donor accepts → QR token issued</b></summary>

```jsonc
// POST /api/v1/donor/blood-requests/1/accept?lat=31.5017&lng=34.4668
// Authorization: Bearer <donor-token>
```

```json
// 201 Created
{
  "id": 1,
  "status": "ACCEPTED",
  "verificationQrCode": "a4fdacff...213859",
  "qrCodeExpiresAt": "2026-08-08T12:00:00",
  "distanceKm": 1.2
}
```

<!-- Replace REPLACE_ME_ACCEPT with the screenshot of the real Swagger/Postman response -->
<p align="center">
  <img src="https://github.com/user-attachments/assets/REPLACE_ME_ACCEPT" width="800" alt="Swagger UI - POST accept endpoint with QR token response"/>
</p>

</details>

---

## 🗄️ Database Schema

17 domain entities + 3 infrastructure entities · 4 Flyway migrations (base schema, seed data, infrastructure tables, enum-storage migration):

```mermaid
erDiagram
    USER ||--o| DONOR : "is (1:1)"
    USER ||--o| ORGANIZATION : "is (1:1)"
    DONOR ||--o| DONOR_HEALTH_PROFILE : has
    DONOR ||--o{ REQUEST_RESPONSE : submits
    DONOR ||--o{ DONOR_ACHIEVEMENT : earns
    ORGANIZATION ||--o{ BLOOD_REQUEST : broadcasts
    BLOOD_REQUEST ||--o{ REQUEST_RESPONSE : receives
    ACHIEVEMENT ||--o{ DONOR_ACHIEVEMENT : "awarded via"
    GOVERNORATE ||--o{ DONOR : "located in"

    USER {
        bigint id PK
        varchar email UK
        varchar role
        boolean isActive
        datetime emailVerifiedAt
    }
    DONOR {
        bigint id PK
        bigint user_id FK
        varchar nationalId UK
        int points
        int level
        double lat
        double lng
    }
    ORGANIZATION {
        bigint id PK
        bigint user_id FK
        varchar orgName
        varchar approvalStatus
        varchar slug UK
    }
    BLOOD_REQUEST {
        bigint id PK
        bigint organization_id FK
        varchar bloodType
        varchar urgencyLevel
        varchar status
        int searchRadiusKm
    }
    REQUEST_RESPONSE {
        bigint id PK
        bigint blood_request_id FK
        bigint donor_id FK
        varchar status
        varchar verificationQrCode
        datetime qrCodeExpiresAt
    }
    DONOR_HEALTH_PROFILE {
        bigint donor_id PK, FK
        varchar bloodType
        boolean chronicDisease
        boolean isEligible
    }
    ACHIEVEMENT {
        bigint id PK
        varchar badgeType
        varchar criteriaType
        int criteriaValue
    }
```

<!-- ===== OPTIONAL SCREENSHOT SLOT: real ERD =====
     Optional but high-value: generate a real ERD from the actual schema
     (MySQL Workbench / DBeaver / JetBrains) and add it beside the mermaid diagram
     to prove the schema is real, not just a drawing. -->
<p align="center">
  <img src="https://github.com/user-attachments/assets/REPLACE_ME_ERD" width="800" alt="Real ERD generated from the database schema"/>
</p>

---

## 🛠️ Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Language | Java 21 | Core language |
| Framework | Spring Boot 3.3.2 | Application framework |
| Security | Spring Security + jjwt 0.12.6 | Stateless JWT auth |
| Persistence | Spring Data JPA / Hibernate | `ddl-auto: validate`, Flyway-managed schema |
| Database | MySQL 8 (prod) / H2 (dev `h2` profile) | Flyway V1–V4 |
| Cache | Redis | Optional JWT blacklist cache |
| ML Integration | WebClient → FastAPI (external, optional) | Custom circuit breaker, 8s timeout |
| QR Codes | ZXing 3.5.2 | Admission verification |
| Docs | springdoc OpenAPI 2.6.0 | Swagger UI |
| Observability | Micrometer + Prometheus + Grafana | Docker Compose services |
| Testing | JUnit 5, Testcontainers (MySQL), H2 | 112 tests |
| Build | Maven | Dependency management |

**Declared but not currently wired** (present in `pom.xml`, not active in the code path): Resilience4j (a custom circuit breaker is used instead), Quartz (`@Scheduled` is used instead), WebSocket, Spring Mail, Freemarker, MapStruct.

---

## 🔒 Security

Enforced across three layers — Spring Security, interceptors, and service-level ownership checks:

- **Stateless JWT** — access + refresh tokens, role-based `@PreAuthorize`
- **Interceptor chain (active, verified)** — email verification gate, donor eligibility gate, organization approval gate, all enforced before controller logic runs
- **Ownership checks** — organization-scoped reads/writes verify the authenticated organization owns the resource; cross-organization access returns 403
- **QR admission security** — `SecureRandom`-generated tokens, 7-day expiry, organization-bound validation
- **Idempotency-key replay protection** on mutating endpoints
- **Rate limiting** on QR scans and public contact submissions
- **Optimistic + pessimistic locking** to prevent race conditions on concurrent donation acceptance

<!-- ===== OPTIONAL SCREENSHOT SLOT: Login → Refresh Token Rotation =====
     Matches the reference format. Capture a real Swagger/Postman flow showing
     POST /v1/auth/login returning access+refresh, then POST /v1/auth/refresh. -->
<details>
<summary><b>📸 Sample: login → refresh token rotation</b></summary>

```jsonc
// POST /api/v1/auth/login
{
  "email": "donor@example.com",
  "password": "********"
}
```

```json
// 200 OK
{
  "token": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 86400000
}
```

<p align="center">
  <img src="https://github.com/user-attachments/assets/REPLACE_ME_LOGIN" width="800" alt="Swagger UI - login and refresh token flow"/>
</p>

</details>

### Known Limitations

| Limitation | Detail |
|---|---|
| Production email verification | The verification gate is enforced, but no production mail-sending flow exists yet — no mail integration, no public verification endpoint. A dev-only `POST /v1/public/verify-dev` endpoint exists strictly under the `h2` profile (`@Profile("h2")` + runtime profile guard, unreachable in production) for local testing. A real token-based email verification flow is required before production rollout. |
| Organization approval bypass | The full approval workflow (PENDING/APPROVED/REJECTED, admin approve/reject endpoints, enforcement interceptor) is implemented, but `register()` currently hardcodes new organizations as `APPROVED` — an intentional demo simplification so organizations are usable immediately. Re-enabling real approvals is a separate design decision. |
| ML scoring service | Disabled by default (`ml-enabled: false`); the FastAPI companion service lives outside this repository. The platform runs fully on the rule-based fallback scorer without it. |
| Rate limiting scope | In-memory, single-instance — not distributed across multiple app instances. |
| Notification delivery | Notifications are persisted to the database only; no email/SMS/push delivery is wired yet. |

---

## 🚀 Getting Started

### Prerequisites
- JDK 21, Maven 3.9+
- Node.js 20+ (frontend)
- Docker (optional, for MySQL/Redis/Prometheus/Grafana via Compose)

### Backend — quick start (H2, no database required)

```bash
mvn package -DskipTests
java -jar target/bloodbridge-1.0.0-SNAPSHOT.jar --spring.profiles.active=h2
```

Starts on `http://localhost:8080/api`, seeded with sample data via `data.sql` + Flyway.

### Backend — MySQL

```bash
# set DB_HOST, DB_PORT, DB_DATABASE, DB_USERNAME, DB_PASSWORD env vars
mvn package
java -jar target/bloodbridge-1.0.0-SNAPSHOT.jar
```

### Frontend

```bash
cd bloodbridge-frontend
npm install
npm run dev   # http://localhost:5173, proxies to /api
```

### Docker Compose

```bash
docker compose up -d mysql redis prometheus grafana app
```
> Note: the compose file also declares an `ai-service` whose build context points to a companion repo not included here — omit that service or provide it separately.

---

## 👤 Author

<table>
  <tr>
    <td align="center" width="300">
      <b>[Your Name]</b><br/>
      <sub>[Your Role]</sub><br/><br/>
      <a href="https://github.com/your-username">
        <img src="https://img.shields.io/badge/GitHub-your--username-181717?style=flat-square&logo=github"/>
      </a>
      <br/>
      <a href="https://www.linkedin.com/in/your-username">
        <img src="https://img.shields.io/badge/LinkedIn-your--username-0A66C2?style=flat-square&logo=linkedin&logoColor=white"/>
      </a>
    </td>
  </tr>
</table>

---

<div align="center">
  <sub>Built phase by phase — Core Domain → Broadcast & Matching → QR Admission → Donor Scoring → Gamification → Security Hardening.</sub>
</div>
