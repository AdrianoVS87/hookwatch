-- V8: Create failure_fingerprints table for recurring error pattern tracking.
-- Fingerprints are SHA-256 hashes of (error_message + span_type + model).
-- Upserted on trace ingestion when a span has FAILED status.

CREATE TABLE IF NOT EXISTS failure_fingerprints (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID        NOT NULL REFERENCES tenants(id),
    agent_id         UUID        NOT NULL REFERENCES agents(id),
    hash             VARCHAR(64) NOT NULL,
    error_message    TEXT,
    span_type        VARCHAR(32),
    model            VARCHAR(128),
    first_seen_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    occurrence_count INT         NOT NULL DEFAULT 1,
    UNIQUE (tenant_id, agent_id, hash)
);

CREATE INDEX idx_fingerprints_agent_id ON failure_fingerprints(agent_id);
CREATE INDEX idx_fingerprints_hash ON failure_fingerprints(hash);
CREATE INDEX idx_fingerprints_occurrence ON failure_fingerprints(agent_id, occurrence_count DESC);
