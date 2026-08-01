-- =====================================================
-- V4: Convert enum-stored columns from integer ordinal to
--     VARCHAR storing the enum's stable `.value` field.
--
-- BACKGROUND:
-- The original schema stored enum columns using JPA's
-- @Enumerated(EnumType.ORDINAL), which writes the enum's
-- *declaration ordinal* (the position in `enum A { X, Y, Z }`)
-- into a TINYINT/INT column.
--
-- The Java enums in this project carry a separate explicit
-- `.value` field (e.g. UserRole.ADMIN has value=3, not the
-- declaration ordinal 0).  The database's actual stored
-- integers therefore reflect the explicit `.value`, not the
-- declaration ordinal.  Any migration that maps by declaration
-- ordinal will silently corrupt every row.
--
-- This migration converts each affected column to VARCHAR(20)
-- and re-writes each row using the enum's explicit `.value` as
-- a string (e.g. "1", "3").  JPA reads/writes these strings via
-- a custom AttributeConverter per enum.  Jackson serializes the
-- enum on the wire as the integer `.value` (see @JsonValue), so
-- the JSON contract is unchanged from the legacy ORDINAL mode.
--
-- AFFECTED COLUMNS (current ordinal → new `.value`-as-string):
--   users.role             value: 1=DONOR, 2=ORGANIZATION, 3=ADMIN
--   donors.gender          value: 1=MALE,   2=FEMALE
--   organizations.approval_status  value: 0=PENDING, 1=APPROVED,
--                                          2=REJECTED, 3=SUSPENDED
--   blood_requests.blood_type      value: 1=O_POSITIVE, 2=O_NEGATIVE,
--                                          3=A_POSITIVE, 4=A_NEGATIVE,
--                                          5=B_POSITIVE, 6=B_NEGATIVE,
--                                          7=AB_POSITIVE,8=AB_NEGATIVE,
--                                          9=UNKNOWN
--   blood_requests.urgency_level   value: 1=NORMAL, 2=CRITICAL
--   blood_requests.status          value: 0=PENDING, 1=BROADCASTED,
--                                          3=FULFILLED, 4=CANCELLED,
--                                          5=EXPIRED   (note: 2 unused)
--   donor_health_profiles.blood_type          (same as blood_type above)
--   donor_health_profiles.verified_blood_type (same as blood_type above)
--   request_responses.status       value: 0=PENDING, 1=ACCEPTED,
--                                          2=DECLINED, 3=COMPLETED,
--                                          4=IGNORED,  5=NO_SHOW,
--                                          6=UNREACHABLE, 7=NOT_NEEDED
--
-- The mappings below preserve the actual meaning of every row
-- (`.value` is what was being stored before), only changing the
-- *type* from integer to VARCHAR.
-- =====================================================

-- ----- users.role (1=DONOR, 2=ORGANIZATION, 3=ADMIN) -----
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL;
UPDATE users SET role = CASE role
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    WHEN '3' THEN '3'
    ELSE role
END;

-- ----- donors.gender (1=MALE, 2=FEMALE) -----
-- Column already VARCHAR(20) but holds digits.
UPDATE donors SET gender = CASE gender
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    ELSE gender
END;

-- ----- organizations.approval_status (0=PENDING,1=APPROVED,2=REJECTED,3=SUSPENDED) -----
ALTER TABLE organizations MODIFY COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT '0';
UPDATE organizations SET approval_status = CASE approval_status
    WHEN '0' THEN '0'
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    WHEN '3' THEN '3'
    ELSE approval_status
END;

-- ----- blood_requests.blood_type (1..9) -----
ALTER TABLE blood_requests MODIFY COLUMN blood_type VARCHAR(20) NOT NULL;
UPDATE blood_requests SET blood_type = CASE blood_type
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    WHEN '3' THEN '3'
    WHEN '4' THEN '4'
    WHEN '5' THEN '5'
    WHEN '6' THEN '6'
    WHEN '7' THEN '7'
    WHEN '8' THEN '8'
    WHEN '9' THEN '9'
    ELSE blood_type
END;

-- ----- blood_requests.urgency_level (1=NORMAL, 2=CRITICAL) -----
ALTER TABLE blood_requests MODIFY COLUMN urgency_level VARCHAR(20) NOT NULL DEFAULT '1';
UPDATE blood_requests SET urgency_level = CASE urgency_level
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    ELSE urgency_level
END;

-- ----- blood_requests.status (0=PENDING,1=BROADCASTED,3=FULFILLED,4=CANCELLED,5=EXPIRED) -----
ALTER TABLE blood_requests MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '0';
UPDATE blood_requests SET status = CASE status
    WHEN '0' THEN '0'
    WHEN '1' THEN '1'
    WHEN '3' THEN '3'
    WHEN '4' THEN '4'
    WHEN '5' THEN '5'
    ELSE status
END;

-- ----- donor_health_profiles.blood_type (1..9, NULLABLE) -----
ALTER TABLE donor_health_profiles MODIFY COLUMN blood_type VARCHAR(20) NULL;
UPDATE donor_health_profiles SET blood_type = CASE blood_type
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    WHEN '3' THEN '3'
    WHEN '4' THEN '4'
    WHEN '5' THEN '5'
    WHEN '6' THEN '6'
    WHEN '7' THEN '7'
    WHEN '8' THEN '8'
    WHEN '9' THEN '9'
    ELSE blood_type
END
WHERE blood_type IS NOT NULL;

-- ----- donor_health_profiles.verified_blood_type (1..9, NULLABLE) -----
ALTER TABLE donor_health_profiles MODIFY COLUMN verified_blood_type VARCHAR(20) NULL;
UPDATE donor_health_profiles SET verified_blood_type = CASE verified_blood_type
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    WHEN '3' THEN '3'
    WHEN '4' THEN '4'
    WHEN '5' THEN '5'
    WHEN '6' THEN '6'
    WHEN '7' THEN '7'
    WHEN '8' THEN '8'
    WHEN '9' THEN '9'
    ELSE verified_blood_type
END
WHERE verified_blood_type IS NOT NULL;

-- ----- request_responses.status (0..7) -----
ALTER TABLE request_responses MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '0';
UPDATE request_responses SET status = CASE status
    WHEN '0' THEN '0'
    WHEN '1' THEN '1'
    WHEN '2' THEN '2'
    WHEN '3' THEN '3'
    WHEN '4' THEN '4'
    WHEN '5' THEN '5'
    WHEN '6' THEN '6'
    WHEN '7' THEN '7'
    ELSE status
END;
