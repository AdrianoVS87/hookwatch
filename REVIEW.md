# Code Review Notes

## Non-Critical Findings

### Performance
- [ ] TraceRepository aggregate queries (avgTokens, avgCost) load via JPQL aggregation; consider native SQL `percentile_cont` for p95 latency in PostgreSQL
- [ ] No pagination on `/api/v1/agents` endpoint — add `Pageable` when agent count grows
- [ ] React Flow canvas re-renders on every span list change; consider memoizing `buildLayout` output

### Security
- [ ] API key stored as plain text in DB — consider hashing (bcrypt) with timing-safe comparison
- [ ] No rate limiting on `/api/v1/tenants` bootstrap endpoint — could be abused to create many tenants
- [ ] SSE `apiKey` query param appears in access logs — consider a short-lived token exchange instead

### Test Coverage
- [ ] TraceService integration tests with Testcontainers missing
- [ ] ApiKeyFilter unit tests missing
- [ ] Frontend: no component tests (Vitest + Testing Library recommended)
- [ ] DataSeeder is `@Profile("dev")` only — no test fixtures for integration test profile

### Code Quality
- [ ] `AgentMetricsDto.p95LatencyMs` always returns 0 — implement percentile query or remove the field until ready
- [ ] DataSeeder spans have null `parentSpanId` — add a seeding pass to link tool→llm spans for a realistic graph
- [ ] `TraceCanvas` re-initialises node/edge state on every `spans` prop change due to `useNodesState(init_n)` — use `useEffect` to sync instead
