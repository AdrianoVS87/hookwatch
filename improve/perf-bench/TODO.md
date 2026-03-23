# TODO: Performance Benchmarks

**Target day:** Saturday
**Branch:** improve/perf-bench
**Effort:** ~3h

## What to implement

### 1. Missing database indexes
Run EXPLAIN ANALYZE on production queries, add missing indexes:
```sql
-- Already needed (identified by Code Review Bot):
CREATE INDEX idx_traces_agent_started ON traces(agent_id, started_at DESC);
CREATE INDEX idx_traces_status ON traces(status);
CREATE INDEX idx_spans_trace_id ON spans(trace_id);
CREATE INDEX idx_tenants_api_key_hash ON tenants(api_key);
```
Add as `V5__add_performance_indexes.sql`

### 2. JMeter/k6 load test
- `perf/k6-load-test.js`: k6 script
- Scenarios: 50 VU, 2 min, mixed read/write
- Thresholds: p95 < 200ms, error rate < 1%

### 3. p95 latency metric endpoint
```java
// GET /api/v1/agents/{id}/metrics returns:
{
  "p50LatencyMs": 45,
  "p95LatencyMs": 180,
  "p99LatencyMs": 320,
  "tracesPerHour": 42,
  "avgCostPerTrace": 0.0034
}
```
Computed via PostgreSQL `percentile_cont(0.95) WITHIN GROUP (ORDER BY duration_ms)`.

### 4. Connection pool tuning
- `application.yml`: HikariCP pool size = 10, timeout = 30s
- Test under load: no connection timeout errors

### 5. Redis cache for agent metrics
- Cache `GET /api/v1/agents/{id}/metrics` for 60s in Redis
- `@Cacheable("agent-metrics")` with `spring-boot-starter-data-redis`
- Cache eviction on new trace creation

## Done criteria
- k6 test: p95 < 200ms at 50 concurrent users
- All 4 indexes applied via Flyway migration
- Redis cache confirmed working (hit/miss logged)
