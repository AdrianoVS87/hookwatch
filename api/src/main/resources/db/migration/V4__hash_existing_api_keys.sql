-- V4: Migrate existing plain-text API keys to BCrypt hashes (strength=12)
--
-- Before this migration, API keys were stored as plain text (UUID strings).
-- After this migration, all API keys are stored as BCrypt hashes.
-- The raw key value is unrecoverable from the hash — existing integrations
-- using the keys listed below will continue to work as the same values are hashed.
--
-- Hashes were pre-computed offline with BCrypt strength=12.
-- $2b$ prefix (Python bcrypt) is accepted by Spring Security's BCryptPasswordEncoder.

UPDATE tenants
SET api_key = '$2b$12$icz1/WK34zNZbPM2n2TUW.o7mhQzic8aOn8RPi4ePjJdK7ne8ix1q'
WHERE api_key = 'demo-key-hookwatch';

UPDATE tenants
SET api_key = '$2b$12$/j3Lm.cz7S3MuObckdk6Tee9uI0m3udmzUtvCv58969.0JSwCg3xe'
WHERE api_key = '4f996db3-8e45-49b6-9fc7-64cd950051bf';

-- Any remaining plain-text keys not matched above are invalidated.
-- Tenants with unknown plain-text keys must rotate via the create-tenant flow.
