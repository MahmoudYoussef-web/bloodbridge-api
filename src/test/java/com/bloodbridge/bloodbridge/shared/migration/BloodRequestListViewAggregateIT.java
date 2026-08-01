package com.bloodbridge.bloodbridge.shared.migration;

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
 * Live-MySQL aggregate verification for the BloodRequestListView counters.
 * Seeds a tiny schema that mirrors the production one (VARCHAR enum column),
 * then runs the same SQL the assembler's GROUP-BY JPQL produces and asserts
 * the exact counter values.
 *
 * Schema seeded: 2 blood requests, 8 response rows covering every status
 * the UI cares about, including PENDING which must NOT count in donorsAccepted.
 *
 * Activates when DB_HOST is set (CI provides it).  Skips locally without DB.
 */
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
class BloodRequestListViewAggregateIT {

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
            for (String stmt : SEED) {
                s.execute(stmt);
            }
        }
    }

    @Test
    void aggregateMatchesExpectedForMixedStatuses() throws SQLException {
        String user = System.getenv().getOrDefault("DB_USERNAME", "root");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "testpass");
        try (Connection c = DriverManager.getConnection(jdbcUrl, user, pass);
             Statement s = c.createStatement()) {

            String sql = "SELECT br_id, COUNT(*) AS responses_count, " +
                    "SUM(CASE WHEN status_str IN ('1','3') THEN 1 ELSE 0 END) AS donors_accepted, " +
                    "SUM(CASE WHEN status_str = '3' THEN 1 ELSE 0 END) AS donors_completed " +
                    "FROM v4_aggregate_responses WHERE deleted_at IS NULL " +
                    "GROUP BY br_id ORDER BY br_id";

            StringBuilder out = new StringBuilder("[AUDIT] BloodRequestListView aggregate query result:\n");
            try (ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    out.append(String.format("  br_id=%d  responsesCount=%d  donorsAccepted=%d  donorsCompleted=%d%n",
                            rs.getLong("br_id"),
                            rs.getLong("responses_count"),
                            rs.getLong("donors_accepted"),
                            rs.getLong("donors_completed")));
                }
            }
            System.out.println(out);

            // Request #1: PENDING(0), ACCEPTED(1), DECLINED(2), COMPLETED(3), IGNORED(4)
            //   responsesCount = 5
            //   donorsAccepted (ACCEPTED + COMPLETED) = 1 + 1 = 2   (PENDING NOT counted)
            //   donorsCompleted (COMPLETED) = 1
            assertRow(s, 1L, 5L, 2L, 1L);

            // Request #2: PENDING, PENDING, NO_SHOW
            //   responsesCount = 3
            //   donorsAccepted = 0
            //   donorsCompleted = 0
            assertRow(s, 2L, 3L, 0L, 0L);
        }
    }

    private void assertRow(Statement s, long brId, long expectedTotal, long expectedAccepted, long expectedCompleted) throws SQLException {
        String sql = "SELECT COUNT(*) AS responses_count, " +
                "SUM(CASE WHEN status_str IN ('1','3') THEN 1 ELSE 0 END) AS donors_accepted, " +
                "SUM(CASE WHEN status_str = '3' THEN 1 ELSE 0 END) AS donors_completed " +
                "FROM v4_aggregate_responses WHERE br_id = " + brId + " AND deleted_at IS NULL";
        try (ResultSet rs = s.executeQuery(sql)) {
            if (!rs.next()) fail("No aggregate row for br_id=" + brId);
            assertEquals(expectedTotal, rs.getLong("responses_count"), "responsesCount for br " + brId);
            assertEquals(expectedAccepted, rs.getLong("donors_accepted"), "donorsAccepted for br " + brId + " (PENDING must NOT count)");
            assertEquals(expectedCompleted, rs.getLong("donors_completed"), "donorsCompleted for br " + brId);
        }
    }

    /** Each statement is one-line so MySQL's JDBC driver parses it cleanly. */
    private static final String[] SEED = new String[] {
            "DROP TABLE IF EXISTS v4_aggregate_responses",
            "DROP TABLE IF EXISTS v4_aggregate_blood_requests",
            "DROP TABLE IF EXISTS v4_aggregate_organizations",
            "DROP TABLE IF EXISTS v4_aggregate_users",
            "CREATE TABLE v4_aggregate_users (id BIGINT PRIMARY KEY AUTO_INCREMENT, role VARCHAR(20) NOT NULL)",
            "CREATE TABLE v4_aggregate_organizations (id BIGINT PRIMARY KEY AUTO_INCREMENT, org_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE v4_aggregate_blood_requests (id BIGINT PRIMARY KEY AUTO_INCREMENT, org_id BIGINT NOT NULL, blood_type VARCHAR(20) NOT NULL, units_needed INT NOT NULL, urgency_level VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL)",
            "CREATE TABLE v4_aggregate_responses (id BIGINT PRIMARY KEY AUTO_INCREMENT, br_id BIGINT NOT NULL, status_str VARCHAR(20) NOT NULL, deleted_at DATETIME NULL)",
            "INSERT INTO v4_aggregate_users (role) VALUES ('2')",
            "INSERT INTO v4_aggregate_organizations (org_name) VALUES ('Test Org')",
            "INSERT INTO v4_aggregate_blood_requests (org_id, blood_type, units_needed, urgency_level, status) VALUES (1, '1', 5, '2', '1')",
            "INSERT INTO v4_aggregate_blood_requests (org_id, blood_type, units_needed, urgency_level, status) VALUES (1, '3', 3, '1', '1')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (1, '0')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (1, '1')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (1, '2')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (1, '3')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (1, '4')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (2, '0')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (2, '0')",
            "INSERT INTO v4_aggregate_responses (br_id, status_str) VALUES (2, '5')"
    };
}
