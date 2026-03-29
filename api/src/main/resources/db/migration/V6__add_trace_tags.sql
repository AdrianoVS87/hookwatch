ALTER TABLE traces
    ADD COLUMN tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

CREATE INDEX idx_traces_tags_gin ON traces USING GIN (tags);

CREATE TABLE annotations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id UUID NOT NULL REFERENCES traces(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    author VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_annotations_trace_id ON annotations(trace_id);
CREATE INDEX idx_annotations_created_at ON annotations(created_at);
