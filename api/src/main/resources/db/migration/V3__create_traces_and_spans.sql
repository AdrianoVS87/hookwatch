CREATE TABLE traces (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id     UUID         NOT NULL REFERENCES agents(id),
    status       VARCHAR(20)  NOT NULL,
    started_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    total_tokens INTEGER,
    total_cost   NUMERIC(12, 6),
    metadata     JSONB
);

CREATE INDEX idx_traces_agent_id ON traces(agent_id);
CREATE INDEX idx_traces_status   ON traces(status);

CREATE TABLE spans (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id       UUID         NOT NULL REFERENCES traces(id) ON DELETE CASCADE,
    parent_span_id UUID,
    name           VARCHAR(255) NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    started_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at   TIMESTAMPTZ,
    input_tokens   INTEGER,
    output_tokens  INTEGER,
    cost           NUMERIC(12, 6),
    model          VARCHAR(100),
    input          TEXT,
    output         TEXT,
    error          TEXT,
    sort_order     INTEGER
);

CREATE INDEX idx_spans_trace_id ON spans(trace_id);
