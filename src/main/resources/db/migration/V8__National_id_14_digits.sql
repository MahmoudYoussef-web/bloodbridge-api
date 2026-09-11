-- V8__National_id_14_digits.sql
-- Egyptian national IDs are 14 digits; the original schema only allowed 9.
ALTER TABLE donors MODIFY national_id VARCHAR(14);
