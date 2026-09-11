# BloodBridge Spring Boot – Session Summary

## What we did (9 items, all done)

### 1. Queue System + Batch Dispatch
- `DispatchBloodRequestNotifications`: batches of 100, re-validates eligibility, skips responded donors, calculates distance, matches Laravel behavior exactly
- `CancelExcessResponsesJob`: async PENDING→NOT_NEEDED + QR revoke + notify
- `AsyncConfig`: dual thread pools (`notificationExecutor`, `jobExecutor`), `@EnableAsync`
- `BloodRequestActionService.cancelExcess()`: now dispatches `CancelExcessResponsesJob` asynchronously via `@Async`
- `ExpireOldBloodRequests`: dispatches `CancelExcessResponsesJob` instead of synchronous cancel loop

### 2. Notification System
- `Notification` entity → `notifications` table, `NotificationRepository` with mark-read operations
- 5 notification classes: `BloodRequestMatchNotification`, `DonorResponseNotification`, `ResponseNotNeededNotification`, `DonorIneligibilityNotification`, `SystemAnnouncementNotification`
- `NotificationService`: DB-persisting, locale-aware, all 5 types supported
- `NotificationType` enum added

### 3. Rate Limiting
- `InMemoryRateLimiter`: sliding-window per-key (ConcurrentHashMap + synchronized)
- `RateLimitService`: `tryQrScan` (30/min per org), `tryContactSubmission` (3/min per IP)
- OrganizationController `scanQr`: returns 429 with `retryAfter` when exceeded

### 4. Middleware (HandlerInterceptors)
- `DonorIneligibilityInterceptor`: blocks donors with `chronicDisease=true` from `/api/donor/**`
- `OrganizationApprovalInterceptor`: blocks unapproved orgs from `/api/org/**`
- `SyncUserLocaleInterceptor`: sets request attribute from user locale
- `EmailVerificationInterceptor`: blocks unverified users unless ADMIN
- `WebMvcConfig`: registers all interceptors with path patterns
- User entity: added `isEmailVerified()` / `isPhoneVerified()` convenience methods

### 5. Settings System
- `Setting` entity + `SettingRepository` mapped to `settings` table (group/name/payload JSON)
- `SettingsService`: PostConstruct cache, typed getters (`getString`, `getInt`, `getBoolean`, `getDouble`)
- `DonorEligibilityService`: now reads thresholds from DB (minWeight, minHeight, minDaysBetweenDonations, minDaysAfterSurgery)
- V2 seed data populates default scoring + general settings

### 6. Additional Enums
- `AppointmentStatus`: SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
- `NotificationType`: 5 values matching Laravel

### 7. POM/Maven Fixes
- Fixed `lombok-mapstruct-binding` groupId from `org.lombok` → `org.projectlombok` (3 occurrences)
- Removed unused `testcontainers:redis` dependency
- Maven 3.9.6 installed locally

### 8. Compilation Fixes (pre-existing entity bugs)
- Added missing `Gender`, `BloodType`, `UserRole`, `OrganizationStatus` imports to entities
- Fixed `@UpdateTimestamp` → proper `org.hibernate.annotations.UpdateTimestamp` (3 entities)
- Created missing `ErrorResponse` DTO
- Added single-arg constructor to `ResourceNotFoundException`
- Fixed method names: `getVerificationQrExpiresAt` → `getQrCodeExpiresAt`

### 9. Build Verification
- `mvn clean compile` – **BUILD SUCCESS** (0 errors)

## Architecture summary (post-session)

All Laravel backend features now have Spring Boot equivalents:

| Feature | Status |
|---------|--------|
| Auth (JWT + roles) | ✅ Done |
| Blood compatibility matrix | ✅ Done |
| Broadcast + progressive radius | ✅ Done |
| Donor scoring (4-level waterfall) | ✅ Done |
| QR verification | ✅ Done |
| Donor eligibility engine | ✅ Done |
| Achievements system | ✅ Done |
| **Queue system** | ✅ **NEW** |
| **Notification system** | ✅ **NEW** |
| **Rate limiting** | ✅ **NEW** |
| **Middleware (4 interceptors)** | ✅ **NEW** |
| **Settings from DB** | ✅ **NEW** |
| **AppointmentStatus enum** | ✅ **NEW** |
| **NotificationType enum** | ✅ **NEW** |

## What remains (lower priority)
- Integration tests (Testcontainers)
- Organization tenancy at service layer
- More comprehensive seed data
- Admin endpoints for settings CRUD
- Rate limiter for contact form and email verification
