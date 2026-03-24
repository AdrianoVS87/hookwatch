CREATE TABLE scores (
    id UUID PRIMARY KEY,
    trace_id UUID NOT NULL REFERENCES traces(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    numeric_value DOUBLE PRECISION,
    string_value VARCHAR(255),
    boolean_value BOOLEAN,
    comment TEXT,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scores_trace_id ON scores(trace_id);
CREATE INDEX idx_scores_name ON scores(name);
