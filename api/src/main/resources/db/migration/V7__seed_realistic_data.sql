-- V7: Realistic seed data for demo
-- Inserts demo tenant and agents if they don't exist, then seeds traces/spans.
-- All inserts use ON CONFLICT DO NOTHING to be idempotent.
-- UUIDs are deterministic to avoid collisions.

-- ─────────────────────────────────────────────────────────────────────────────
-- Ensure demo tenant + agents exist (idempotent)
-- ─────────────────────────────────────────────────────────────────────────────

-- Demo tenant
INSERT INTO tenants (id, name, api_key, created_at)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'HookWatch Demo',
    '$2b$12$icz1/WK34zNZbPM2n2TUW.o7mhQzic8aOn8RPi4ePjJdK7ne8ix1q',
    '2026-01-01 00:00:00+00'
)
ON CONFLICT (id) DO NOTHING;

-- OpenClaw Assistant agent
INSERT INTO agents (id, tenant_id, name, description, created_at)
VALUES (
    'b1c2d3e4-f5a6-7890-bcde-f01234567891',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'OpenClaw Assistant',
    'AI coding assistant powered by Claude',
    '2026-01-15 08:00:00+00'
)
ON CONFLICT (id) DO NOTHING;

-- Code Review Bot
INSERT INTO agents (id, tenant_id, name, description, created_at)
VALUES (
    'c1d2e3f4-a5b6-7890-cdef-012345678902',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'Code Review Bot',
    'Automated PR reviewer',
    '2026-01-20 08:00:00+00'
)
ON CONFLICT (id) DO NOTHING;

-- Documentation Bot
INSERT INTO agents (id, tenant_id, name, description, created_at)
VALUES (
    'd1e2f3a4-b5c6-7890-defa-123456789099',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'Documentation Bot',
    'Auto-generates documentation from code',
    '2026-01-25 08:00:00+00'
)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- Seed traces and spans
-- ─────────────────────────────────────────────────────────────────────────────

DO $$
DECLARE
    v_agent_openclaw  UUID := 'b1c2d3e4-f5a6-7890-bcde-f01234567891';
    v_agent_codereview UUID := 'c1d2e3f4-a5b6-7890-cdef-012345678902';
    v_agent_docbot    UUID := 'd1e2f3a4-b5c6-7890-defa-123456789099';

    -- model distribution weights (cumulative for rand selection)
    -- opus: 0.15, sonnet: 0.65 (cum), haiku: 0.90 (cum), codex: 1.00 (cum)

    v_day             DATE;
    v_day_of_week     INT;   -- 0=Sunday, 1=Monday...6=Saturday
    v_daily_count     INT;
    v_trace_idx       INT;
    v_model_rand      FLOAT;
    v_model           TEXT;
    v_task            TEXT;
    v_input_tokens    INT;
    v_output_tokens   INT;
    v_total_tokens    INT;
    v_cost            NUMERIC(12,6);
    v_status          TEXT;
    v_status_rand     FLOAT;
    v_started_at      TIMESTAMPTZ;
    v_duration_secs   INT;
    v_completed_at    TIMESTAMPTZ;
    v_trace_id        UUID;
    v_span_count      INT;
    v_span_idx        INT;
    v_span_id         UUID;
    v_span_type       TEXT;
    v_span_name       TEXT;
    v_span_tokens_in  INT;
    v_span_tokens_out INT;
    v_span_cost       NUMERIC(12,6);
    v_span_dur        INT;

    -- Task lists for each agent
    v_openclaw_tasks  TEXT[] := ARRAY[
        'architecture-review','code-generation','debugging','refactoring',
        'security-audit','test-writing','documentation','api-design',
        'performance-optimization','code-review','data-pipeline','infra-setup'
    ];
    v_codereview_tasks TEXT[] := ARRAY[
        'pr-review','style-check','security-scan','complexity-analysis',
        'dependency-check','test-coverage','lint-fix','type-check'
    ];
    v_docbot_tasks    TEXT[] := ARRAY[
        'readme-generation','api-docs','changelog','inline-comments',
        'tutorial-writing','architecture-diagram','runbook','sdk-docs'
    ];

    v_span_types      TEXT[] := ARRAY['LLM_CALL','LLM_CALL','LLM_CALL','TOOL_CALL','RETRIEVAL'];
    v_span_names_llm  TEXT[] := ARRAY['llm-generate','llm-analyze','llm-summarize','llm-classify'];
    v_span_names_tool TEXT[] := ARRAY['tool-search','tool-read-file','tool-write-file','tool-exec','tool-fetch'];
    v_span_names_ret  TEXT[] := ARRAY['retrieval-embed','retrieval-search','retrieval-rerank'];

BEGIN

    -- ═════════════════════════════════════════════════════════════════════════
    -- OpenClaw Assistant: ~300 traces over 2026-02-01 to 2026-03-27
    -- ═════════════════════════════════════════════════════════════════════════
    v_day := '2026-02-01'::DATE;
    v_trace_idx := 0;

    WHILE v_day <= '2026-03-27'::DATE LOOP
        v_day_of_week := EXTRACT(DOW FROM v_day)::INT;  -- 0=Sun, 6=Sat

        -- Tuned to land around ~300 traces over the 2026-02-01..2026-03-27 window
        -- Weekdays: 5-9 traces, weekends: 1-3
        IF v_day_of_week IN (0, 6) THEN
            v_daily_count := 1 + floor(random() * 3)::INT;
        ELSE
            v_daily_count := 5 + floor(random() * 5)::INT;
        END IF;

        FOR i IN 1..v_daily_count LOOP
            v_trace_idx := v_trace_idx + 1;

            -- Deterministic UUID from seed
            v_trace_id := md5('openclaw-trace-' || v_day::TEXT || '-' || i::TEXT)::UUID;

            -- Model selection (weighted random)
            v_model_rand := random();
            -- Real API pricing (per 1M tokens, March 2026):
            -- Opus 4.6:   $5 input / $25 output
            -- Sonnet 4.6: $3 input / $15 output
            -- Haiku 4.5:  $1 input / $5 output
            -- Codex 5.3:  $1.75 input / $14 output

            IF v_model_rand < 0.15 THEN
                v_model := 'claude-opus-4-6';
                v_input_tokens  := 60000 + floor(random() * 80000)::INT;
                v_output_tokens := 20000 + floor(random() * 40000)::INT;
                v_cost := round((v_input_tokens / 1000000.0 * 5.0 + v_output_tokens / 1000000.0 * 25.0)::NUMERIC, 6);
            ELSIF v_model_rand < 0.65 THEN
                v_model := 'claude-sonnet-4-6';
                v_input_tokens  := 12000 + floor(random() * 38000)::INT;
                v_output_tokens := 8000  + floor(random() * 22000)::INT;
                v_cost := round((v_input_tokens / 1000000.0 * 3.0 + v_output_tokens / 1000000.0 * 15.0)::NUMERIC, 6);
            ELSIF v_model_rand < 0.90 THEN
                v_model := 'claude-haiku-4-5';
                v_input_tokens  := 3000  + floor(random() * 10000)::INT;
                v_output_tokens := 2000  + floor(random() * 5000)::INT;
                v_cost := round((v_input_tokens / 1000000.0 * 1.0 + v_output_tokens / 1000000.0 * 5.0)::NUMERIC, 6);
            ELSE
                v_model := 'codex-5.3';
                v_input_tokens  := 30000 + floor(random() * 60000)::INT;
                v_output_tokens := 20000 + floor(random() * 40000)::INT;
                v_cost := round((v_input_tokens / 1000000.0 * 1.75 + v_output_tokens / 1000000.0 * 14.0)::NUMERIC, 6);
            END IF;

            v_total_tokens := v_input_tokens + v_output_tokens;

            -- Status distribution: 85% COMPLETED, 10% FAILED, 5% RUNNING
            v_status_rand := random();
            IF v_status_rand < 0.85 THEN
                v_status := 'COMPLETED';
            ELSIF v_status_rand < 0.95 THEN
                v_status := 'FAILED';
            ELSE
                v_status := 'RUNNING';
            END IF;

            -- Randomize start time within business hours (8-22)
            v_started_at := (v_day::TIMESTAMPTZ + (8 * 3600 + floor(random() * 50400)::INT) * INTERVAL '1 second');
            v_duration_secs := 5 + floor(random() * 300)::INT;

            IF v_status = 'RUNNING' THEN
                v_completed_at := NULL;
            ELSE
                v_completed_at := v_started_at + (v_duration_secs * INTERVAL '1 second');
            END IF;

            -- Task from list
            v_task := v_openclaw_tasks[1 + floor(random() * array_length(v_openclaw_tasks, 1))::INT];

            INSERT INTO traces (id, agent_id, status, started_at, completed_at, total_tokens, total_cost, metadata)
            VALUES (
                v_trace_id,
                v_agent_openclaw,
                v_status,
                v_started_at,
                v_completed_at,
                v_total_tokens,
                round(v_cost::NUMERIC, 6),
                jsonb_build_object('model', v_model, 'task', v_task, 'seed', 'v7')
            )
            ON CONFLICT (id) DO NOTHING;

            -- Insert 2-5 spans for this trace
            v_span_count := 2 + floor(random() * 4)::INT;

            FOR s IN 1..v_span_count LOOP
                v_span_id := md5('openclaw-span-' || v_trace_id::TEXT || '-' || s::TEXT)::UUID;

                -- First span is always LLM_CALL, rest vary
                IF s = 1 THEN
                    v_span_type := 'LLM_CALL';
                ELSE
                    v_span_type := v_span_types[1 + floor(random() * array_length(v_span_types, 1))::INT];
                END IF;

                IF v_span_type = 'LLM_CALL' THEN
                    v_span_name     := v_span_names_llm[1 + floor(random() * array_length(v_span_names_llm, 1))::INT];
                    v_span_tokens_in  := v_input_tokens / v_span_count;
                    v_span_tokens_out := v_output_tokens / v_span_count;
                    v_span_cost     := round((v_cost / v_span_count)::NUMERIC, 6);
                ELSIF v_span_type = 'TOOL_CALL' THEN
                    v_span_name     := v_span_names_tool[1 + floor(random() * array_length(v_span_names_tool, 1))::INT];
                    v_span_tokens_in  := NULL;
                    v_span_tokens_out := NULL;
                    v_span_cost     := NULL;
                ELSE
                    v_span_name     := v_span_names_ret[1 + floor(random() * array_length(v_span_names_ret, 1))::INT];
                    v_span_tokens_in  := floor(random() * 2000)::INT;
                    v_span_tokens_out := NULL;
                    v_span_cost     := round((random() * 0.01)::NUMERIC, 6);
                END IF;

                v_span_dur := floor(v_duration_secs / v_span_count * random() * 2)::INT;

                INSERT INTO spans (
                    id, trace_id, parent_span_id, name, type, status,
                    started_at, completed_at,
                    input_tokens, output_tokens, cost, model, sort_order
                )
                VALUES (
                    v_span_id,
                    v_trace_id,
                    NULL,
                    v_span_name,
                    v_span_type,
                    CASE WHEN v_status = 'FAILED' AND s = v_span_count THEN 'FAILED'
                         WHEN v_status = 'RUNNING' AND s = v_span_count THEN 'RUNNING'
                         ELSE 'COMPLETED' END,
                    v_started_at + ((s - 1) * v_span_dur * INTERVAL '1 second'),
                    CASE WHEN v_status = 'RUNNING' AND s = v_span_count THEN NULL
                         ELSE v_started_at + (s * v_span_dur * INTERVAL '1 second') END,
                    v_span_tokens_in,
                    v_span_tokens_out,
                    v_span_cost,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN v_model ELSE NULL END,
                    s
                )
                ON CONFLICT (id) DO NOTHING;
            END LOOP;

        END LOOP;

        v_day := v_day + 1;
    END LOOP;

    -- ═════════════════════════════════════════════════════════════════════════
    -- Code Review Bot: ~100 traces over 2026-02-01 to 2026-03-27
    -- ═════════════════════════════════════════════════════════════════════════
    v_day := '2026-02-01'::DATE;

    WHILE v_day <= '2026-03-27'::DATE LOOP
        v_day_of_week := EXTRACT(DOW FROM v_day)::INT;

        IF v_day_of_week IN (0, 6) THEN
            v_daily_count := floor(random() * 3)::INT;  -- 0-2 on weekends
        ELSE
            v_daily_count := 1 + floor(random() * 4)::INT;  -- 1-4 weekdays
        END IF;

        FOR i IN 1..v_daily_count LOOP
            v_trace_id := md5('codereview-trace-' || v_day::TEXT || '-' || i::TEXT)::UUID;

            v_model_rand := random();
            IF v_model_rand < 0.20 THEN
                v_model := 'claude-opus-4-6';
                v_input_tokens  := 30000 + floor(random() * 50000)::INT;
                v_output_tokens := 5000  + floor(random() * 20000)::INT;
                v_cost          := 1.00  + random() * 4.00;
            ELSIF v_model_rand < 0.70 THEN
                v_model := 'claude-sonnet-4-6';
                v_input_tokens  := 8000  + floor(random() * 30000)::INT;
                v_output_tokens := 3000  + floor(random() * 15000)::INT;
                v_cost          := 0.10  + random() * 1.20;
            ELSIF v_model_rand < 0.95 THEN
                v_model := 'claude-haiku-4-5';
                v_input_tokens  := 2000  + floor(random() * 8000)::INT;
                v_output_tokens := 500   + floor(random() * 3000)::INT;
                v_cost          := 0.005 + random() * 0.07;
            ELSE
                v_model := 'codex-5.3';
                v_input_tokens  := 20000 + floor(random() * 40000)::INT;
                v_output_tokens := 8000  + floor(random() * 20000)::INT;
                v_cost          := 0.30  + random() * 1.50;
            END IF;

            v_total_tokens := v_input_tokens + v_output_tokens;

            v_status_rand := random();
            IF v_status_rand < 0.88 THEN
                v_status := 'COMPLETED';
            ELSIF v_status_rand < 0.97 THEN
                v_status := 'FAILED';
            ELSE
                v_status := 'RUNNING';
            END IF;

            v_started_at   := (v_day::TIMESTAMPTZ + (9 * 3600 + floor(random() * 36000)::INT) * INTERVAL '1 second');
            v_duration_secs := 3 + floor(random() * 120)::INT;
            v_completed_at  := CASE WHEN v_status = 'RUNNING' THEN NULL
                                    ELSE v_started_at + (v_duration_secs * INTERVAL '1 second') END;

            v_task := v_codereview_tasks[1 + floor(random() * array_length(v_codereview_tasks, 1))::INT];

            INSERT INTO traces (id, agent_id, status, started_at, completed_at, total_tokens, total_cost, metadata)
            VALUES (
                v_trace_id,
                v_agent_codereview,
                v_status,
                v_started_at,
                v_completed_at,
                v_total_tokens,
                round(v_cost::NUMERIC, 6),
                jsonb_build_object('model', v_model, 'task', v_task, 'seed', 'v7')
            )
            ON CONFLICT (id) DO NOTHING;

            v_span_count := 2 + floor(random() * 3)::INT;
            FOR s IN 1..v_span_count LOOP
                v_span_id   := md5('codereview-span-' || v_trace_id::TEXT || '-' || s::TEXT)::UUID;
                v_span_type := CASE WHEN s = 1 THEN 'LLM_CALL' ELSE v_span_types[1 + floor(random() * 5)::INT] END;
                v_span_name := CASE v_span_type
                    WHEN 'LLM_CALL'  THEN v_span_names_llm[1 + floor(random() * 4)::INT]
                    WHEN 'TOOL_CALL' THEN v_span_names_tool[1 + floor(random() * 5)::INT]
                    ELSE                  v_span_names_ret[1 + floor(random() * 3)::INT] END;
                v_span_dur  := floor(v_duration_secs / v_span_count * random() * 2)::INT;

                INSERT INTO spans (
                    id, trace_id, parent_span_id, name, type, status,
                    started_at, completed_at, input_tokens, output_tokens, cost, model, sort_order
                )
                VALUES (
                    v_span_id, v_trace_id, NULL, v_span_name, v_span_type,
                    CASE WHEN v_status = 'FAILED' AND s = v_span_count THEN 'FAILED'
                         WHEN v_status = 'RUNNING' AND s = v_span_count THEN 'RUNNING'
                         ELSE 'COMPLETED' END,
                    v_started_at + ((s-1) * v_span_dur * INTERVAL '1 second'),
                    CASE WHEN v_status = 'RUNNING' AND s = v_span_count THEN NULL
                         ELSE v_started_at + (s * v_span_dur * INTERVAL '1 second') END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN v_input_tokens / v_span_count ELSE NULL END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN v_output_tokens / v_span_count ELSE NULL END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN round((v_cost / v_span_count)::NUMERIC, 6) ELSE NULL END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN v_model ELSE NULL END,
                    s
                )
                ON CONFLICT (id) DO NOTHING;
            END LOOP;
        END LOOP;

        v_day := v_day + 1;
    END LOOP;

    -- ═════════════════════════════════════════════════════════════════════════
    -- Documentation Bot: ~100 traces over 2026-02-01 to 2026-03-27
    -- ═════════════════════════════════════════════════════════════════════════
    v_day := '2026-02-01'::DATE;

    WHILE v_day <= '2026-03-27'::DATE LOOP
        v_day_of_week := EXTRACT(DOW FROM v_day)::INT;

        IF v_day_of_week IN (0, 6) THEN
            v_daily_count := floor(random() * 3)::INT;
        ELSE
            v_daily_count := 1 + floor(random() * 4)::INT;
        END IF;

        FOR i IN 1..v_daily_count LOOP
            v_trace_id := md5('docbot-trace-' || v_day::TEXT || '-' || i::TEXT)::UUID;

            v_model_rand := random();
            IF v_model_rand < 0.10 THEN
                v_model := 'claude-opus-4-6';
                v_input_tokens  := 20000 + floor(random() * 40000)::INT;
                v_output_tokens := 8000  + floor(random() * 20000)::INT;
                v_cost          := 0.80  + random() * 3.00;
            ELSIF v_model_rand < 0.55 THEN
                v_model := 'claude-sonnet-4-6';
                v_input_tokens  := 6000  + floor(random() * 25000)::INT;
                v_output_tokens := 4000  + floor(random() * 12000)::INT;
                v_cost          := 0.08  + random() * 0.90;
            ELSIF v_model_rand < 0.90 THEN
                v_model := 'claude-haiku-4-5';
                v_input_tokens  := 1500  + floor(random() * 7000)::INT;
                v_output_tokens := 1000  + floor(random() * 4000)::INT;
                v_cost          := 0.003 + random() * 0.06;
            ELSE
                v_model := 'codex-5.3';
                v_input_tokens  := 15000 + floor(random() * 35000)::INT;
                v_output_tokens := 6000  + floor(random() * 18000)::INT;
                v_cost          := 0.25  + random() * 1.20;
            END IF;

            v_total_tokens := v_input_tokens + v_output_tokens;

            v_status_rand := random();
            IF v_status_rand < 0.90 THEN
                v_status := 'COMPLETED';
            ELSIF v_status_rand < 0.97 THEN
                v_status := 'FAILED';
            ELSE
                v_status := 'RUNNING';
            END IF;

            v_started_at   := (v_day::TIMESTAMPTZ + (7 * 3600 + floor(random() * 57600)::INT) * INTERVAL '1 second');
            v_duration_secs := 4 + floor(random() * 200)::INT;
            v_completed_at  := CASE WHEN v_status = 'RUNNING' THEN NULL
                                    ELSE v_started_at + (v_duration_secs * INTERVAL '1 second') END;

            v_task := v_docbot_tasks[1 + floor(random() * array_length(v_docbot_tasks, 1))::INT];

            INSERT INTO traces (id, agent_id, status, started_at, completed_at, total_tokens, total_cost, metadata)
            VALUES (
                v_trace_id,
                v_agent_docbot,
                v_status,
                v_started_at,
                v_completed_at,
                v_total_tokens,
                round(v_cost::NUMERIC, 6),
                jsonb_build_object('model', v_model, 'task', v_task, 'seed', 'v7')
            )
            ON CONFLICT (id) DO NOTHING;

            v_span_count := 2 + floor(random() * 3)::INT;
            FOR s IN 1..v_span_count LOOP
                v_span_id   := md5('docbot-span-' || v_trace_id::TEXT || '-' || s::TEXT)::UUID;
                v_span_type := CASE WHEN s = 1 THEN 'LLM_CALL' ELSE v_span_types[1 + floor(random() * 5)::INT] END;
                v_span_name := CASE v_span_type
                    WHEN 'LLM_CALL'  THEN v_span_names_llm[1 + floor(random() * 4)::INT]
                    WHEN 'TOOL_CALL' THEN v_span_names_tool[1 + floor(random() * 5)::INT]
                    ELSE                  v_span_names_ret[1 + floor(random() * 3)::INT] END;
                v_span_dur  := floor(v_duration_secs / v_span_count * random() * 2)::INT;

                INSERT INTO spans (
                    id, trace_id, parent_span_id, name, type, status,
                    started_at, completed_at, input_tokens, output_tokens, cost, model, sort_order
                )
                VALUES (
                    v_span_id, v_trace_id, NULL, v_span_name, v_span_type,
                    CASE WHEN v_status = 'FAILED' AND s = v_span_count THEN 'FAILED'
                         WHEN v_status = 'RUNNING' AND s = v_span_count THEN 'RUNNING'
                         ELSE 'COMPLETED' END,
                    v_started_at + ((s-1) * v_span_dur * INTERVAL '1 second'),
                    CASE WHEN v_status = 'RUNNING' AND s = v_span_count THEN NULL
                         ELSE v_started_at + (s * v_span_dur * INTERVAL '1 second') END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN v_input_tokens / v_span_count ELSE NULL END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN v_output_tokens / v_span_count ELSE NULL END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN round((v_cost / v_span_count)::NUMERIC, 6) ELSE NULL END,
                    CASE WHEN v_span_type = 'LLM_CALL' THEN v_model ELSE NULL END,
                    s
                )
                ON CONFLICT (id) DO NOTHING;
            END LOOP;
        END LOOP;

        v_day := v_day + 1;
    END LOOP;

END;
$$;
