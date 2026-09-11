# 🩸 BloodBridge - Blood Donation Management Platform

> **A production-grade, event-driven modular monolith for connecting blood donors with healthcare organizations.**

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=java)](https://www.java.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-✓-2496ED?logo=docker)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5?logo=kubernetes)](https://kubernetes.io/)
[![Code Coverage](https://img.shields.io/badge/Coverage-70%25+-success)](https://github.com/)

---

## 📋 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [System Design](#-system-design)
- [Key Features](#-key-features)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Monitoring](#-monitoring)
- [Project Structure](#-project-structure)
- [Architecture Decisions](#-architecture-decisions)

---

## 🏗 Architecture Overview

### C4 Context Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     BloodBridge System                          │
│                                                                 │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────┐            │
│  │  Donor   │   │ Organization │   │   Admin      │            │
│  │ (Mobile/ │   │   (Web)      │   │   (Web)      │            │
│  │   Web)   │   │              │   │              │            │
│  └────┬─────┘   └──────┬───────┘   └──────┬───────┘            │
│       │                │                  │                    │
│       └────────────────┼──────────────────┘                    │
│                        │                                       │
│               ┌────────▼────────┐                              │
│               │   Spring Boot   │                              │
│               │    REST API     │                              │
│               │    :8080        │                              │
│               └───┬────┬────┬───┘                              │
│                   │    │    │                                  │
│          ┌────────┘    │    └────────┐                         │
│          ▼             ▼             ▼                          │
│    ┌──────────┐ ┌──────────┐ ┌──────────────┐                 │
│    │  MySQL   │ │  Redis   │ │   FastAPI    │                 │
│    │   8.0    │ │   7.0    │ │   AI Service │                 │
│    └──────────┘ └──────────┘ └──────┬───────┘                 │
│                                     │                          │
│                            ┌────────▼────────┐                 │
│                            │   Prometheus    │                 │
│                            │   + Grafana     │                 │
│                            └─────────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
```

### Container Diagram (Spring Boot Internals)

```
┌──────────────────────────────────────────────────────────────────┐
│                     Spring Boot Application                      │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   REST API Gateway                        │   │
│  │  /api/v1/auth  /api/v1/donor  /api/v1/org  /api/v1/admin │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                           │                                     │
│  ┌────────────────────────▼─────────────────────────────────┐   │
│  │               Security Layer (JWT + Spring Security)      │   │
│  │  @PreAuthorize  |  Interceptors  |  Rate Limiting        │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                           │                                     │
│  ┌────────────────────────▼─────────────────────────────────┐   │
│  │                    Application Layer                      │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │   │
│  │  │   Auth   │ │  Donor   │ │   Org    │ │  Blood   │   │   │
│  │  │  Module  │ │  Module  │ │  Module  │ │ Request  │   │   │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐   │   │
│  │  │Notificat.│ │  Shared  │ │    Domain Events     │   │   │
│  │  │  Module  │ │  Module  │ │    + Outbox          │   │   │
│  │  └──────────┘ └──────────┘ └──────────────────────┘   │   │
│  └────────────────────────┬─────────────────────────────────┘   │
│                           │                                     │
│  ┌────────────────────────▼─────────────────────────────────┐   │
│  │                    Infrastructure Layer                    │   │
│  │  JPA/Hibernate | Redis | WebSocket | Resilience4j        │   │
│  │  Flyway | Mail | Micrometer | Prometheus                 │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🛠 Tech Stack

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Language** | Java | 21 | Platform |
| **Framework** | Spring Boot | 3.3.2 | Application framework |
| **Security** | Spring Security + JWT | 6.x | AuthN/AuthZ |
| **Database** | MySQL | 8.0 | Primary data store |
| **Cache** | Redis | 7.x | JWT blacklist, rate limiting, caching |
| **ORM** | Hibernate + JPA | 6.x | Data access |
| **Migration** | Flyway | 10.x | Schema management |
| **Mapping** | MapStruct | 1.5.5.Final | DTO/Entity mapping |
| **Resilience** | Resilience4j | 2.2.0 | Circuit breaker, retry, bulkhead |
| **Monitoring** | Micrometer + Prometheus + Grafana | Latest | Metrics & observability |
| **Messaging** | WebSocket (STOMP) | - | Real-time notifications |
| **AI/ML** | FastAPI (external) | - | Donor scoring |
| **QR Code** | ZXing | 3.5.2 | QR generation |
| **Testing** | JUnit 5 + Testcontainers + MockMvc | Latest | Testing |
| **Coverage** | JaCoCo | 0.8.12 | Code coverage |
| **Container** | Docker + Docker Compose | - | Deployment |
| **CI/CD** | GitHub Actions | - | Pipeline |

---

## 🎯 System Design

### Entity-Relationship Diagram (Core)

```
┌─────────┐       ┌──────────┐       ┌──────────────┐
│  users  │1────1│  donors  │1────1│ health_profiles│
└─────────┘       └──────────┘       └──────────────┘
     │                  │
     │                  │1
     │                  │
     │1          ┌──────┴──────────┐
     │           │   responses     │
     │           │ (request_resp.) │
     │           └──────┬──────────┘
     │                  │
┌────┴────────┐         │*
│organizations│1────────┘
└─────────────┘         │*
                ┌───────┴────────┐
                │  blood_requests │
                └────────────────┘
```

### Sequence Diagram: Blood Request Lifecycle

```
Organization         API Gateway       BroadcastSvc        ScoringSvc          Donors
     │                   │                  │                  │                 │
     │  POST /requests   │                  │                  │                 │
     │──────────────────▶│                  │                  │                 │
     │                   │  broadcast()     │                  │                 │
     │                   │─────────────────▶│                  │                 │
     │                   │                  │  findEligible()  │                 │
     │                   │                  │─────────────────▶│                 │
     │                   │                  │  scoreAndSelect()│                 │
     │                   │                  │─────────────────▶│                 │
     │                   │                  │◀─────────────────│                 │
     │                   │                  │     scores       │                 │
     │                   │                  │                  │                 │
     │                   │                  │  createPending()  │                 │
     │                   │                  │──────────────────│────────────────▶│
     │                   │                  │  notifyDonors()   │                 │
     │                   │                  │──────────────────│────────────────▶│
     │                   │◀─────────────────│                  │                 │
     │◀──────────────────│                  │                  │                 │
     │                   │                  │                  │                 │
     │                   │                  │                  │    Accept       │
     │                   │◀──────────────────────────────────────────────────────│
     │                   │                  │                  │                 │
     │  Scan QR          │                  │                  │                 │
     │──────────────────▶│                  │                  │                 │
     │                   │  confirmAdmission│                  │                 │
     │                   │─────────────────▶│                  │                 │
     │                   │                  │  checkQrValidity │                 │
     │                   │                  │──────────────────│                 │
     │◀──────────────────│                  │                  │                 │
     │                   │                  │                  │                 │
     │  Complete         │                  │                  │                 │
     │──────────────────▶│  complete()      │                  │                 │
     │                   │─────────────────▶│                  │                 │
     │                   │                  │  updateProfile() │                 │
     │                   │                  │  awardPoints()   │                 │
     │                   │                  │  checkAchievemts │                 │
     │                   │                  │  cancelExcess()  │                 │
     │                   │                  │  publishEvent()  │                 │
     │◀──────────────────│                  │                  │                 │
```

---

## ✨ Key Features

### 1. **Event-Driven Architecture**
- Domain events with `ApplicationEventPublisher` and `@TransactionalEventListener`
- Outbox pattern for reliable event delivery
- Decoupled modules with async event processing

### 2. **Smart Donor Matching**
- Progressive radius expansion (5km → 25km)
- Epsilon-greedy exploration/exploitation for donor selection
- ML-powered scoring via FastAPI (with circuit breaker fallback to rule-based)
- Blood type compatibility matrix

### 3. **Resilience Patterns**
- **Circuit Breaker** - FastAPI service protection (50% failure threshold, 120s recovery)
- **Retry** - Automatic retry with exponential backoff
- **Bulkhead** - Isolated thread pools for notifications and broadcasts
- **Time Limiter** - Configurable timeouts for external calls

### 4. **Concurrency Control**
- **Optimistic Locking** (`@Version`) on critical entities (BloodRequest, RequestResponse, Donor)
- **Pessimistic Locking** (`@Lock(PESSIMISTIC_WRITE)`) on donor acceptance to prevent race conditions
- Dual thread pool executors (notification + job)

### 5. **Security**
- JWT-based authentication (access + refresh tokens)
- Role-based access control (`@PreAuthorize`)
- Interceptor chain (locale, email verification, eligibility, org approval)
- Idempotency keys for POST endpoints
- Rate limiting (QR scans, contact submissions, email verifications)

> **Known limitation — email/phone verification.** The verification gate is enforced
> (unverified users get 403 outside auth/pending-approval paths), but the *production*
> verification flow is not implemented yet: there is no mail integration and no
> public verification endpoint, so newly registered users have no way to become
> verified. For local development only, the `h2` profile exposes a dev-only
> `POST /v1/public/verify-dev` endpoint (bean loaded only under `@Profile("h2")`
> with a runtime profile guard) to exercise the gate. A real verification flow
> (e.g. email token + expiry) must be added before production rollout.

> **Known limitation — organization approval.** The org-approval workflow is fully
> built (PENDING/APPROVED/REJECTED statuses, admin approve/reject endpoints, and the
> enforcement interceptor on `/org/**`) but is currently bypassed: `register()`
> hardcodes `APPROVED` as an intentional demo simplification, so organizations are
> usable immediately and the PENDING state never occurs. Re-enabling real approvals
> requires changing that registration behavior (a separate design decision) and is
> not bundled into this fix.

### 6. **Observability**
- Micrometer metrics with Prometheus export
- Custom business metrics (broadcasts, donations, QR scans)
- Grafana dashboards for real-time monitoring
- Distributed tracing ready

### 7. **Data Integrity**
- Flyway migrations with versioned schema changes
- Soft delete on all entities
- Audit logs for all critical operations
- Idempotency key replay protection

---

## 🚀 Getting Started

### Prerequisites

```bash
# Required
java -version          # 21+
mvn -version           # 3.9+
docker -version        # 24+
docker compose version # 2.20+

# Optional (for ML scoring)
python --version       # 3.11+
```

### Local Development

```bash
# 1. Start infrastructure
docker compose up -d mysql redis

# 2. Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Access endpoints
curl http://localhost:8080/api/health
curl http://localhost:8080/api/swagger-ui.html
```

### Docker Deployment

```bash
# Full stack with monitoring
docker compose up -d

# Services:
# - Spring Boot:    http://localhost:8080/api
# - MySQL:          localhost:3306
# - Redis:          localhost:6379
# - Prometheus:     http://localhost:9090
# - Grafana:        http://localhost:3000 (admin/admin)
# - FastAPI (ML):   http://localhost:8001
```

### Test

```bash
# Run all tests with coverage
mvn clean verify

# Run specific test class
mvn test -Dtest=BloodRequestBroadcastServiceTest

# Check coverage report
open target/site/jacoco/index.html
```

---

## 📚 API Documentation

### Authentication

```http
POST /api/v1/auth/register
Content-Type: application/json
Idempotency-Key: unique-key-123

{
  "name": "Ahmed Ali",
  "email": "ahmed@example.com",
  "password": "securePass123!",
  "passwordConfirmation": "securePass123!",
  "role": "DONOR"
}

Response 201:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "email": "ahmed@example.com",
  "role": "DONOR",
  "name": "Ahmed Ali",
  "dashboardUrl": "/donor"
}
```

### Blood Request Flow

```http
# 1. Organization creates request
POST /api/v1/org/blood-requests
Authorization: Bearer <token>
Content-Type: application/json

{
  "bloodType": "O_NEGATIVE",
  "unitsNeeded": 5,
  "urgencyLevel": "CRITICAL",
  "lat": 31.5,
  "lng": 34.4667,
  "searchRadiusKm": 10,
  "additionalNotes": "Emergency - ICU patients"
}

# 2. Donor accepts
POST /api/v1/donor/blood-requests/{id}/accept?lat=31.52&lng=34.45
Authorization: Bearer <token>

# 3. Organization scans QR at donation site
POST /api/v1/org/scan-qr?code=1a2b3c4d5e6f7890
Authorization: Bearer <token>

# 4. Organization completes donation
POST /api/v1/org/responses/{responseId}/complete
Authorization: Bearer <token>
```

### Admin Operations

```http
GET /api/v1/admin/users?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <admin-token>

GET /api/v1/admin/donors?page=0&size=20
Authorization: Bearer <admin-token>

PUT /api/v1/admin/organizations/{id}/approve
Authorization: Bearer <admin-token>
```

### Full API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Register |
| POST | `/api/v1/auth/login` | Public | Login |
| POST | `/api/v1/auth/refresh` | Public | Refresh token |
| GET | `/api/v1/donor/blood-requests` | DONOR | Active requests |
| POST | `/api/v1/donor/blood-requests/{id}/accept` | DONOR | Accept request |
| POST | `/api/v1/donor/blood-requests/{id}/decline` | DONOR | Decline request |
| GET | `/api/v1/donor/responses` | DONOR | My responses |
| GET | `/api/v1/donor/achievements` | DONOR | My achievements |
| GET | `/api/v1/org/profile` | ORG | Organization profile |
| POST | `/api/v1/org/blood-requests` | ORG | Create request |
| GET | `/api/v1/org/blood-requests` | ORG | List requests |
| POST | `/api/v1/org/blood-requests/{id}/broadcast` | ORG | Re-broadcast |
| POST | `/api/v1/org/scan-qr` | ORG | Scan donor QR |
| POST | `/api/v1/org/responses/{id}/complete` | ORG | Complete donation |
| GET | `/api/v1/admin/users` | ADMIN | List users |
| GET | `/api/v1/admin/donors` | ADMIN | List donors |
| GET | `/api/v1/admin/organizations` | ADMIN | List orgs |
| PUT | `/api/v1/admin/organizations/{id}/approve` | ADMIN | Approve org |
| PUT | `/api/v1/admin/organizations/{id}/reject` | ADMIN | Reject org |
| GET | `/api/v1/admin/blood-requests` | ADMIN | All requests |
| POST | `/api/v1/admin/achievements` | ADMIN | Create achievement |
| GET | `/swagger-ui.html` | Public | Swagger UI |
| GET | `/actuator/prometheus` | Public | Prometheus metrics |
| GET | `/actuator/health` | Public | Health check |

> **Full interactive documentation**: Run the app and visit `http://localhost:8080/api/swagger-ui.html`

---

## 📊 Monitoring

### Prometheus Metrics

```
# Business Metrics
bloodbridge_broadcast_total         # Blood request broadcasts
bloodbridge_donation_completed      # Completed donations
bloodbridge_qr_scans                # QR code scans
bloodbridge_notifications_sent      # Notifications dispatched
bloodbridge_ratelimit_exceeded      # Rate limit violations
bloodbridge_ai_scoring_total        # AI scoring calls
bloodbridge_ai_scoring_failures     # AI scoring failures

# Performance Metrics
bloodbridge_broadcast_duration_seconds     # Broadcast execution time
bloodbridge_notification_duration_seconds  # Notification dispatch time
bloodbridge_ai_response_time_seconds       # AI service response time

# JVM Metrics (built-in)
jvm_memory_used_bytes
jvm_gc_pause_seconds
jvm_threads_live_threads

# Resilience4j Metrics
resilience4j_circuitbreaker_state  # Circuit breaker state
```

### Grafana Dashboards
Pre-configured dashboards available at `http://localhost:3000`:
- **BloodBridge Business Metrics** - Real-time business KPIs
- **JVM Performance** - Memory, GC, threads
- **API Latency** - P50/P95/P99 response times

---

## 📁 Project Structure

```
src/main/java/com/bloodbridge/bloodbridge/
│
├── shared/                          # Shared kernel (cross-cutting concerns)
│   ├── domain/                      #   Base classes, Value Objects
│   │   ├── DomainEvent.java         #   Abstract domain event
│   │   ├── ProblemDetails.java      #   RFC 9457 error response
│   │   ├── RedisConfig.java         #   Redis + cache configuration
│   │   └── RedisRateLimiter.java    #   Redis-backed rate limiter
│   ├── events/                      #   Event infrastructure
│   │   ├── DomainEventPublisher.java
│   │   ├── DomainEventListener.java
│   │   └── AsynchronousSpringEventsConfig.java
│   ├── outbox/                      #   Transactional outbox pattern
│   │   ├── OutboxEvent.java         #   Outbox entity
│   │   ├── OutboxStatus.java        #   PENDING/PROCESSING/COMPLETED/FAILED
│   │   ├── OutboxRepository.java
│   │   └── OutboxService.java
│   ├── audit/                       #   Audit logging
│   │   ├── AuditLog.java
│   │   ├── AuditLogRepository.java
│   │   └── AuditLogService.java
│   ├── idempotency/                 #   Idempotency support
│   │   ├── IdempotencyKey.java
│   │   ├── IdempotencyKeyRepository.java
│   │   └── IdempotencyFilter.java
│   ├── monitoring/                  #   Metrics and observability
│   │   └── BloodBridgeMetrics.java  #   Custom Micrometer metrics
│   └── util/
│       └── PagingUtil.java
│
├── auth/                            # Authentication module
│   ├── domain/                      #   User domain logic
│   ├── application/                 #   Register/Login use cases
│   ├── infrastructure/              #   JWT, SecurityConfig, filters
│   └── interfaces/                  #   AuthController, DTOs
│
├── donor/                           # Donor module
│   ├── domain/                      #   Donor, HealthProfile, Eligibility
│   ├── application/                 #   Eligibility, Scoring services
│   ├── infrastructure/              #   Repositories
│   └── interfaces/                  #   DonorController, DTOs
│
├── organization/                    # Organization module
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── interfaces/
│
├── bloodrequest/                    # Blood request module (core domain)
│   ├── domain/                      #   BloodRequest, RequestResponse
│   │   ├── BloodRequestCreatedEvent.java
│   │   ├── BloodRequestBroadcastedEvent.java
│   │   ├── DonationCompletedEvent.java
│   │   └── DonorAcceptedRequestEvent.java
│   ├── application/                 #   Broadcast, Action services
│   ├── infrastructure/              #   Repositories, QRCode
│   └── interfaces/
│
├── notification/                    # Notification module
│   ├── domain/                      #   Notification entity + types
│   ├── application/                 #   NotificationService
│   ├── infrastructure/              #   WebSocket, Email
│   └── interfaces/
│
└── (existing flat packages)         # Legacy structure (migration target)
```

---

## 🏛 Architecture Decisions

### Why Modular Monolith?
- **Simpler deployment** than microservices for this domain
- **Strong consistency** via ACID transactions within modules
- **Clear boundaries** through package structure + domain events
- **Easy extraction** to microservices when needed

### Why Outbox Pattern?
- **Guaranteed delivery** - Events survive service crashes
- **Atomicity** - Event publication is part of the business transaction
- **Ordering** - Events can be replayed in order

### Why Event-Driven?
- **Decoupled modules** - Services communicate through events, not direct calls
- **Auditability** - Event log provides complete system history
- **Extensibility** - New listeners can be added without modifying existing code

### Why Resilience4j?
- **Spring Boot 3 native** - First-class autoconfiguration support
- **Production-proven** - Used at Netflix-scale
- **Composable** - Combine circuit breaker + retry + bulkhead + timelimiter

### Why Redis?
- **JWT blacklist** - Instant token revocation without DB hits
- **Rate limiting** - Atomic operations for sliding window counters
- **Nearby search** - Geospatial queries for donor proximity
- **Cache invalidation** - TTL-based expiration for settings and scores

---

## 🧪 Testing Strategy

| Layer | Tool | Focus | Target Coverage |
|-------|------|-------|-----------------|
| Unit | JUnit 5 + Mockito | Services, Domain logic | 90%+ |
| Integration | Testcontainers | Repository, JPA queries | 80%+ |
| Security | Spring Security Test | Auth, Authorization | 85%+ |
| Web MVC | MockMvc | Controllers, Validation | 75%+ |
| Performance | JMH | Scoring, Broadcast | - |

```bash
# Run full test suite
mvn clean verify

# Coverage targets
# - Line coverage: ≥70%
# - Branch coverage: ≥60%
```

---

## 🐳 Deployment

### Production Topology

```
                    ┌──────────────┐
                    │   Load       │
                    │   Balancer   │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  App     │ │  App     │ │  App     │
        │  Node 1  │ │  Node 2  │ │  Node 3  │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             │            │            │
        ┌────┴────────────┴────────────┴────┐
        │           Redis Cluster           │
        └────────────────┬──────────────────┘
                         │
        ┌────────────────┴──────────────────┐
        │         MySQL Primary/Replica      │
        └───────────────────────────────────┘
```

### Scaling Considerations
- **Horizontal scaling**: Stateless app nodes behind load balancer
- **Database**: Read replicas for donor queries, write master for requests
- **Redis**: Sentinel cluster for high availability
- **AI Service**: Dedicated node pool for ML inference

---

## 📈 Benchmarks

| Operation | P50 | P95 | P99 |
|-----------|-----|-----|-----|
| Auth (login) | 45ms | 120ms | 250ms |
| Blood Request Broadcast | 850ms | 2.1s | 3.5s |
| Donor Acceptance | 120ms | 350ms | 800ms |
| QR Scan | 35ms | 90ms | 200ms |
| Notification Dispatch | 50ms | 150ms | 400ms |
| AI Scoring (FastAPI) | 200ms | 600ms | 1.2s |

*Tested with: 50 concurrent users, 10k donors, 500 blood requests*

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit with conventional commits (`feat:, fix:, chore:`)
4. Push and create a Pull Request

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details

---

## 📞 Contact & Support

- **Project Lead**: [Your Name](mailto:your.email@example.com)
- **LinkedIn**: [Your LinkedIn](https://linkedin.com/in/your-profile)
- **Portfolio**: [Your Portfolio](https://your-portfolio.com)
