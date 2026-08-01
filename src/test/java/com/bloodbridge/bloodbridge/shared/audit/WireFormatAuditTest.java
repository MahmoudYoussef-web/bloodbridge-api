package com.bloodbridge.bloodbridge.shared.audit;

import com.bloodbridge.bloodbridge.dto.AchievementView;
import com.bloodbridge.bloodbridge.dto.BloodRequestLiteView;
import com.bloodbridge.bloodbridge.dto.BloodRequestListView;
import com.bloodbridge.bloodbridge.dto.DonorAchievementView;
import com.bloodbridge.bloodbridge.dto.DonorAchievementsResponse;
import com.bloodbridge.bloodbridge.dto.DonorLiteView;
import com.bloodbridge.bloodbridge.dto.RequestResponseView;
import com.bloodbridge.bloodbridge.enumtype.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-Jackson verification of the wire format the frontend will receive.
 * Boots no Spring context, hits no endpoint — it directly serializes the
 * same DTO instances the controllers return, so we can see byte-for-byte
 * what the network response body will look like.
 *
 * This is the audit artifact the frontend reviewer should compare
 * against bloodbridge-frontend/src/types/index.ts and the page-level
 * TypeScript expectations.
 */
class WireFormatAuditTest {

    private final ObjectMapper m = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // -------- Step 2: enum wire format --------

    @Test
    void userRole_serializesAsInteger() throws Exception {
        String json = m.writeValueAsString(UserRole.DONOR);
        assertEquals("1", json, "UserRole.DONOR must serialize as integer 1");
        assertEquals(UserRole.DONOR, m.readValue("1", UserRole.class), "int 1 -> DONOR");
        assertEquals(UserRole.DONOR, m.readValue("\"1\"", UserRole.class), "string \"1\" -> DONOR");
        assertEquals(UserRole.ADMIN, m.readValue("3", UserRole.class), "int 3 -> ADMIN (not 0/ordinal)");
    }

    @Test
    void bloodType_serializesAsInteger() throws Exception {
        String json = m.writeValueAsString(BloodType.A_POSITIVE);
        assertEquals("3", json, "BloodType.A_POSITIVE must serialize as integer 3");
        assertEquals(BloodType.A_POSITIVE, m.readValue("3", BloodType.class), "int 3 -> A_POSITIVE");
        assertEquals(BloodType.A_NEGATIVE, m.readValue("4", BloodType.class), "int 4 -> A_NEGATIVE");
        assertEquals(BloodType.UNKNOWN, m.readValue("9", BloodType.class), "int 9 -> UNKNOWN");
    }

    @Test
    void gender_serializesAsInteger() throws Exception {
        assertEquals("1", m.writeValueAsString(Gender.MALE));
        assertEquals("2", m.writeValueAsString(Gender.FEMALE));
        assertEquals(Gender.MALE, m.readValue("1", Gender.class));
        assertEquals(Gender.FEMALE, m.readValue("2", Gender.class));
    }

    @Test
    void bloodRequestStatus_serializesAsIntegerWithGaps() throws Exception {
        // Critical: BloodRequestStatus.FULFILLED has value=3 (not ordinal 2),
        // there is no value=2 (it's a gap).  Frontend's statusLabels uses
        // 0,1,3,4,5 keys — this MUST still match.
        assertEquals("0", m.writeValueAsString(BloodRequestStatus.PENDING));
        assertEquals("1", m.writeValueAsString(BloodRequestStatus.BROADCASTED));
        assertEquals("3", m.writeValueAsString(BloodRequestStatus.FULFILLED),
                "BloodRequestStatus.FULFILLED value=3 (gap), must serialize as 3");
        assertEquals("4", m.writeValueAsString(BloodRequestStatus.CANCELLED));
        assertEquals("5", m.writeValueAsString(BloodRequestStatus.EXPIRED));
    }

    @Test
    void urgencyLevel_serializesAsInteger() throws Exception {
        assertEquals("1", m.writeValueAsString(UrgencyLevel.NORMAL));
        assertEquals("2", m.writeValueAsString(UrgencyLevel.CRITICAL));
    }

    @Test
    void organizationStatus_serializesAsInteger() throws Exception {
        assertEquals("0", m.writeValueAsString(OrganizationStatus.PENDING));
        assertEquals("1", m.writeValueAsString(OrganizationStatus.APPROVED));
        assertEquals("2", m.writeValueAsString(OrganizationStatus.REJECTED));
        assertEquals("3", m.writeValueAsString(OrganizationStatus.SUSPENDED));
    }

    @Test
    void requestResponseStatus_serializesAsInteger() throws Exception {
        assertEquals("0", m.writeValueAsString(RequestResponseStatus.PENDING));
        assertEquals("1", m.writeValueAsString(RequestResponseStatus.ACCEPTED));
        assertEquals("2", m.writeValueAsString(RequestResponseStatus.DECLINED));
        assertEquals("3", m.writeValueAsString(RequestResponseStatus.COMPLETED));
        assertEquals("4", m.writeValueAsString(RequestResponseStatus.IGNORED));
        assertEquals("5", m.writeValueAsString(RequestResponseStatus.NO_SHOW));
        assertEquals("6", m.writeValueAsString(RequestResponseStatus.UNREACHABLE));
        assertEquals("7", m.writeValueAsString(RequestResponseStatus.NOT_NEEDED));
    }

    // -------- Step 3: achievements wire format --------

    @Test
    void achievementsResponse_exactShape() throws Exception {
        AchievementView earnedAchievement = new AchievementView(
                7L,
                "{\"en\":\"First Donation\",\"ar\":\"تبرع أول\"}",
                "{\"en\":\"Donated once\"}",
                10, "bronze.png", "bronze", "donations", 1, 1);

        DonorAchievementView earned = new DonorAchievementView(
                42L, 5L, 7L,
                LocalDateTime.of(2024, 3, 15, 9, 30).toString(),
                earnedAchievement);

        AchievementView locked = new AchievementView(
                8L,
                "{\"en\":\"Five Donations\"}",
                "{\"en\":\"Donated five times\"}",
                50, "silver.png", "silver", "donations", 5, 2);

        DonorAchievementsResponse resp = DonorAchievementsResponse.builder()
                .earned(List.of(earned))
                .locked(List.of(locked))
                .points(150)
                .level(3)
                .build();

        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(resp);
        System.out.println("[AUDIT] /donor/achievements body =\n" + json);

        // Re-parse and confirm field-by-field.  Frontend reads:
        //   earned[i].id, .donorId, .achievementId, .earnedAt,
        //             .achievement.{id,name.en,name.ar,description.en,
        //                          pointsRewards,badgeIcon,badgeType,
        //                          criteriaType,criteriaValue,displayOrder}
        //   locked[i].{id,name.en,description.en,pointsRewards,badgeType,
        //              criteriaType,criteriaValue,displayOrder}
        //   top-level points, level
        var root = m.readTree(json);
        var first = root.path("earned").get(0);
        assertEquals(42, first.path("id").asInt(), "earned[0].id (DonorAchievement row id, used as React key)");
        assertEquals(5, first.path("donorId").asInt());
        assertEquals(7, first.path("achievementId").asInt());
        assertEquals("2024-03-15T09:30", first.path("earnedAt").asText().substring(0, 16));
        var inner = first.path("achievement");
        assertEquals(7, inner.path("id").asInt());
        assertEquals(true, inner.path("name").isObject(), "achievement.name must be a JSON object, not a string");
        assertEquals("First Donation", inner.path("name").path("en").asText(),
                "achievement.name.en must be readable as a sub-property — this is what the UI reads");
        assertEquals(true, inner.path("description").isObject());
        assertEquals(10, inner.path("pointsRewards").asInt());
        assertEquals("bronze.png", inner.path("badgeIcon").asText());
        assertEquals("bronze", inner.path("badgeType").asText());
        assertEquals("donations", inner.path("criteriaType").asText());
        assertEquals(1, inner.path("criteriaValue").asInt());
        assertEquals(1, inner.path("displayOrder").asInt());

        var firstLocked = root.path("locked").get(0);
        assertEquals(8, firstLocked.path("id").asInt());
        assertEquals(true, firstLocked.path("name").isObject());
        assertEquals("Five Donations", firstLocked.path("name").path("en").asText());
        assertEquals("donations", firstLocked.path("criteriaType").asText());

        assertEquals(150, root.path("points").asInt());
        assertEquals(3, root.path("level").asInt());
    }

    @Test
    void achievementsResponse_noLeakedEntityFields() throws Exception {
        AchievementView ach = new AchievementView(
                1L, "{\"en\":\"x\"}", "{\"en\":\"y\"}",
                1, "i", "t", "c", 1, 1);
        DonorAchievementView earned = new DonorAchievementView(1L, 1L, 1L, "2024-01-01T00:00:00", ach);
        DonorAchievementsResponse resp = DonorAchievementsResponse.builder()
                .earned(List.of(earned)).locked(List.of()).points(0).level(1).build();

        String json = m.writeValueAsString(resp);
        // Confirm none of the entity-leaked fields appear.
        for (String leaked : new String[]{"meta", "awardedBy", "donor", "deletedAt"}) {
            assertEquals(false, json.contains("\"" + leaked + "\""),
                    "Wire format must NOT contain leaked entity field: " + leaked);
        }
    }

    // -------- D2: RequestResponseView wire format (live endpoints only) --------

    @Test
    void requestResponseView_containsEveryFrontendReadField() throws Exception {
        BloodRequestLiteView br = new BloodRequestLiteView(
                7L, 1, 2,
                new BloodRequestLiteView.OrganizationLiteView("Cairo Hospital"));
        DonorLiteView donor = new DonorLiteView(new DonorLiteView.UserLiteView("Ahmed Donor"));
        RequestResponseView row = new RequestResponseView(
                42L, 9L, 1, "2024-03-15T09:30:00", "2024-03-15T10:00:00", "2024-03-15T09:30:00",
                br, donor);

        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(row));
        System.out.println("[AUDIT] /donor/responses + /org/responses body =\n" + json);

        var root = m.readTree(json).get(0);

        // DonorHistory.tsx:27  — r.bloodRequest?.organization?.orgName
        assertEquals("Cairo Hospital", root.path("bloodRequest").path("organization").path("orgName").asText());
        // DonorHistory.tsx:28  — r.bloodRequest.bloodType (used as array index)
        assertEquals(1, root.path("bloodRequest").path("bloodType").asInt());
        // DonorHistory.tsx:29  — r.bloodRequest?.unitsNeeded
        assertEquals(2, root.path("bloodRequest").path("unitsNeeded").asInt());
        // DonorHistory:30, DonorDashboard:14..18, OrgDashboard:18, OrgStatistics:14/85/87 — r.status
        assertEquals(1, root.path("status").asInt(),
                "status must be the enum .value (1 = ACCEPTED), not declaration ordinal");
        // DonorHistory:31, DonorDashboard:18/22 — r.respondedAt
        assertEquals("2024-03-15T09:30:00", root.path("respondedAt").asText());
        // OrgDashboard:18 — r.verifiedAt
        assertEquals("2024-03-15T10:00:00", root.path("verifiedAt").asText());
        // OrgStatistics:17/77 — r.createdAt
        assertEquals("2024-03-15T09:30:00", root.path("createdAt").asText());
        // OrgStatistics:78 — r.id (React key)
        assertEquals(42, root.path("id").asLong());
        // OrgStatistics:81 — r.donor?.user?.name
        assertEquals("Ahmed Donor", root.path("donor").path("user").path("name").asText());
        // OrgScanQR.tsx:94 — r.bloodRequestId
        assertEquals(9L, root.path("bloodRequestId").asLong());

        // No entity-leaked fields
        for (String leaked : new String[]{
                "donorId", "verificationQrCode", "qrCodeExpiresAt",
                "declineReason", "distance", "deletedAt", "version", "updatedAt",
                "correctionUsedAt", "appointmentId", "createdBy", "awardedBy"
        }) {
            assertEquals(false, json.contains("\"" + leaked + "\""),
                    "RequestResponseView wire format must NOT leak entity field: " + leaked);
        }
    }

    // -------- D3: BloodRequestListView wire format (live endpoints only) --------

    @Test
    void bloodRequestListView_containsEveryFrontendReadField() throws Exception {
        BloodRequestListView br = new BloodRequestListView(
                11L,           // id
                1,             // bloodType (O_POSITIVE)
                5,             // unitsNeeded
                2,             // urgencyLevel (CRITICAL)
                1,             // status (BROADCASTED)
                10,            // searchRadiusKm
                "Urgent O+ needed for pediatric patient",
                java.time.LocalDateTime.of(2024, 3, 15, 9, 30),
                7L,            // responsesCount
                3L,            // donorsAccepted  (ACCEPTED + COMPLETED)
                1L);           // donorsCompleted (COMPLETED only)

        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(br));
        System.out.println("[AUDIT] /org/blood-requests body =\n" + json);

        var root = m.readTree(json).get(0);

        // OrgBloodRequests.tsx:41 — r.id (link)
        assertEquals(11, root.path("id").asLong());
        // OrgBloodRequests:29 / OrgViewBloodRequest:38 / OrgDashboard:60 / OrgStatistics:25 — r.bloodType
        assertEquals(1, root.path("bloodType").asInt());
        // OrgBloodRequests:30 / OrgViewBloodRequest:41 / OrgDashboard:63 — r.unitsNeeded
        assertEquals(5, root.path("unitsNeeded").asInt());
        // OrgBloodRequests:31 / OrgViewBloodRequest:39 / OrgDashboard:64 — r.urgencyLevel
        assertEquals(2, root.path("urgencyLevel").asInt());
        // OrgBloodRequests:32 / OrgViewBloodRequest:40,61 / OrgDashboard:68 / OrgStatistics:12 — r.status
        assertEquals(1, root.path("status").asInt());
        // OrgViewBloodRequest:44 — r.searchRadiusKm
        assertEquals(10, root.path("searchRadiusKm").asInt());
        // OrgViewBloodRequest:54,57 — r.additionalNotes
        assertEquals("Urgent O+ needed for pediatric patient", root.path("additionalNotes").asText());
        // OrgBloodRequests:39 / OrgViewBloodRequest:45 — r.createdAt
        assertEquals("2024-03-15T09:30", root.path("createdAt").asText().substring(0, 16));
        // OrgBloodRequests:33 — r.responsesCount (NEW)
        assertEquals(7L, root.path("responsesCount").asLong());
        // OrgViewBloodRequest:42 — r.donorsAccepted (NEW)
        assertEquals(3L, root.path("donorsAccepted").asLong(),
                "donorsAccepted = ACCEPTED + COMPLETED only, PENDING excluded");
        // OrgBloodRequests:35,36 / OrgViewBloodRequest:43 — r.donorsCompleted (NEW)
        assertEquals(1L, root.path("donorsCompleted").asLong());

        // No entity-leaked fields
        for (String leaked : new String[]{
                "organizationId", "organization", "lat", "lng", "locationAddress",
                "broadcastedAt", "fulfilledAt", "actualSearchRadiusKm",
                "distance", "myStatus", "version", "deletedAt", "updatedAt",
                "verificationQrCode", "awardedBy"
        }) {
            assertEquals(false, json.contains("\"" + leaked + "\""),
                    "BloodRequestListView wire format must NOT leak entity field: " + leaked);
        }
    }
}
