-- =====================================================
-- V5: Add email verification token columns to users.
--
-- PURPOSE:
-- Support a real token-based email verification flow.
-- On registration a random token is stored with an expiry;
-- the user verifies it through a dedicated endpoint
-- (POST /v1/auth/verify-email) instead of relying on the
-- dev-only /v1/public/verify-dev probe.
--
-- NULL values: pre-existing rows simply have never
-- requested verification (or registered before this
-- migration), which is semantically "not verified yet".
-- =====================================================

ALTER TABLE users ADD COLUMN verification_token VARCHAR(255) NULL AFTER remember_token;
ALTER TABLE users ADD COLUMN verification_token_expires_at TIMESTAMP NULL AFTER verification_token;
