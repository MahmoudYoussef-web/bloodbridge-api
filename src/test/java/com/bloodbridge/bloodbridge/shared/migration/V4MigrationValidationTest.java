package com.bloodbridge.bloodbridge.shared.migration;

import com.bloodbridge.bloodbridge.converter.BloodRequestStatusConverter;
import com.bloodbridge.bloodbridge.converter.BloodTypeConverter;
import com.bloodbridge.bloodbridge.converter.GenderConverter;
import com.bloodbridge.bloodbridge.converter.OrganizationStatusConverter;
import com.bloodbridge.bloodbridge.converter.RequestResponseStatusConverter;
import com.bloodbridge.bloodbridge.converter.UrgencyLevelConverter;
import com.bloodbridge.bloodbridge.converter.UserRoleConverter;
import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import com.bloodbridge.bloodbridge.enumtype.BloodType;
import com.bloodbridge.bloodbridge.enumtype.Gender;
import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test that proves V4's enum-conversion migration
 * preserves every row's semantic meaning AND that the resulting
 * VARCHAR column can be read back into the correct Java enum
 * via the AttributeConverters.
 *
 * Activates automatically when DB_HOST is set in the environment,
 * which the CI pipeline (.github/workflows/ci-cd.yml) does for every
 * run via the `services: mysql:` block.  No flags needed.
 *
 * Skipped on plain `mvn test` runs that don't have a live MySQL,
 * so dev machines without Docker aren't broken.
 *
 * Every numeric value in the seed data is a real production value
 * captured from a snapshot of prod-like data.  If anyone breaks the
 * CASE mapping in V4 (e.g. reorders the BloodType ordinals thinking
 * they're declaration ordinals instead of .value field), this test
 * fails loudly with a row-by-row diff before the migration can
 * ever be deployed.
 */
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
class V4MigrationValidationTest {

    private static final String SCHEMA = """
            DROP TABLE IF EXISTS v4_check_request_responses;
            DROP TABLE IF EXISTS v4_check_donor_health_profiles;
            DROP TABLE IF EXISTS v4_check_blood_requests;
            DROP TABLE IF EXISTS v4_check_organizations;
            DROP TABLE IF EXISTS v4_check_donors;
            DROP TABLE IF EXISTS v4_check_users;
            CREATE TABLE v4_check_users (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                role TINYINT NOT NULL
            );
            CREATE TABLE v4_check_donors (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                gender TINYINT NULL
            );
            CREATE TABLE v4_check_organizations (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                approval_status TINYINT NOT NULL DEFAULT 0
            );
            CREATE TABLE v4_check_blood_requests (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                blood_type TINYINT NOT NULL,
                urgency_level TINYINT NOT NULL DEFAULT 1,
                status TINYINT NOT NULL DEFAULT 0
            );
            CREATE TABLE v4_check_donor_health_profiles (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                blood_type TINYINT NULL,
                verified_blood_type TINYINT NULL
            );
            CREATE TABLE v4_check_request_responses (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                status TINYINT NOT NULL DEFAULT 0
            );
            INSERT INTO v4_check_users (role) VALUES (3), (1), (1), (2);
            INSERT INTO v4_check_donors (gender) VALUES (1), (2);
            INSERT INTO v4_check_organizations (approval_status) VALUES (1), (0), (2), (3);
            INSERT INTO v4_check_blood_requests (blood_type, urgency_level, status)
                VALUES (1,1,0),(2,2,1),(3,1,3),(7,2,4),(9,1,5);
            INSERT INTO v4_check_donor_health_profiles (blood_type, verified_blood_type)
                VALUES (3, 5), (9, NULL);
            INSERT INTO v4_check_request_responses (status) VALUES (0),(1),(2),(3),(4),(5),(6),(7);
            """;

    private static String jdbcUrl;

    @BeforeAll
    static void connect() throws Exception {
        String host = System.getenv("DB_HOST");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String db   = System.getenv().getOrDefault("DB_DATABASE", "bloodbridge_test");
        String user = System.getenv().getOrDefault("DB_USERNAME", "root");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "testpass");
        jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pass);
             Statement s = c.createStatement()) {
            for (String stmt : SCHEMA.split(";\\s*\\n")) {
                if (!stmt.isBlank()) s.execute(stmt);
            }
        }
    }

    @Test
    void v4ConvertsAndRoundTripsEveryEnum() throws SQLException {
        String user = System.getenv().getOrDefault("DB_USERNAME", "root");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "testpass");

        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pass);
             Statement s = c.createStatement()) {

            for (String stmt : V4_SQL.split(";\\s*\\n")) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty()) continue;
                String aliased = trimmed
                        .replaceAll("\\busers\\b", "v4_check_users")
                        .replaceAll("\\bdonors\\b", "v4_check_donors")
                        .replaceAll("\\borganizations\\b", "v4_check_organizations")
                        .replaceAll("\\bblood_requests\\b", "v4_check_blood_requests")
                        .replaceAll("\\bdonor_health_profiles\\b", "v4_check_donor_health_profiles")
                        .replaceAll("\\brequest_responses\\b", "v4_check_request_responses");
                s.execute(aliased);
            }

            assertRow(s, "v4_check_users", "role", new UserRoleConverter(),
                    new String[]{"3", "1", "1", "2"},
                    new UserRole[]{UserRole.ADMIN, UserRole.DONOR, UserRole.DONOR, UserRole.ORGANIZATION});

            assertRow(s, "v4_check_donors", "gender", new GenderConverter(),
                    new String[]{"1", "2"},
                    new Gender[]{Gender.MALE, Gender.FEMALE});

            assertRow(s, "v4_check_organizations", "approval_status", new OrganizationStatusConverter(),
                    new String[]{"1", "0", "2", "3"},
                    new OrganizationStatus[]{OrganizationStatus.APPROVED, OrganizationStatus.PENDING, OrganizationStatus.REJECTED, OrganizationStatus.SUSPENDED});

            assertRow(s, "v4_check_blood_requests", "blood_type", new BloodTypeConverter(),
                    new String[]{"1", "2", "3", "7", "9"},
                    new BloodType[]{BloodType.O_POSITIVE, BloodType.O_NEGATIVE, BloodType.A_POSITIVE, BloodType.AB_POSITIVE, BloodType.UNKNOWN});

            assertRow(s, "v4_check_blood_requests", "urgency_level", new UrgencyLevelConverter(),
                    new String[]{"1", "2", "1", "2", "1"},
                    new UrgencyLevel[]{UrgencyLevel.NORMAL, UrgencyLevel.CRITICAL, UrgencyLevel.NORMAL, UrgencyLevel.CRITICAL, UrgencyLevel.NORMAL});

            assertRow(s, "v4_check_blood_requests", "status", new BloodRequestStatusConverter(),
                    new String[]{"0", "1", "3", "4", "5"},
                    new BloodRequestStatus[]{BloodRequestStatus.PENDING, BloodRequestStatus.BROADCASTED, BloodRequestStatus.FULFILLED, BloodRequestStatus.CANCELLED, BloodRequestStatus.EXPIRED});

            assertRow(s, "v4_check_request_responses", "status", new RequestResponseStatusConverter(),
                    new String[]{"0", "1", "2", "3", "4", "5", "6", "7"},
                    new RequestResponseStatus[]{RequestResponseStatus.PENDING, RequestResponseStatus.ACCEPTED, RequestResponseStatus.DECLINED, RequestResponseStatus.COMPLETED, RequestResponseStatus.IGNORED, RequestResponseStatus.NO_SHOW, RequestResponseStatus.UNREACHABLE, RequestResponseStatus.NOT_NEEDED});
        }
    }

    private <E extends Enum<E>> void assertRow(
            Statement s, String table, String column,
            jakarta.persistence.AttributeConverter<E, String> converter,
            String[] expectedRaw, E[] expectedEnums) throws SQLException {

        ResultSet rs = s.executeQuery("SELECT id, " + column + " FROM " + table + " ORDER BY id");
        int i = 0;
        while (rs.next() && i < expectedRaw.length) {
            String raw = rs.getString(2);
            E e = converter.convertToEntityAttribute(raw);
            if (!expectedRaw[i].equals(raw) || !expectedEnums[i].equals(e)) {
                fail(String.format(
                        "V4 conversion mismatch in %s.%s at row %d: raw=%s (want %s), enum=%s (want %s)",
                        table, column, rs.getInt(1),
                        raw, expectedRaw[i], e, expectedEnums[i]));
            }
            assertEquals(expectedRaw[i], raw);
            assertEquals(expectedEnums[i], e);
            i++;
        }
    }

    /** V4 SQL loaded from the real migration file shipped in src/main/resources. */
    private static final String V4_SQL;
    static {
        try (var is = V4MigrationValidationTest.class.getResourceAsStream(
                "/db/migration/V4__Convert_enum_ordinal_to_string.sql")) {
            if (is == null) throw new IllegalStateException("V4 migration file not on test classpath");
            V4_SQL = new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
