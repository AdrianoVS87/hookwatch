# HookWatch API Reference

Base URL: `http://localhost:8080` (dev) | `https://your-api-domain.com` (prod)  
Interactive docs: `GET /swagger-ui/index.html`

All endpoints (except tenant bootstrap) require:
```
X-API-Key: <your-api-key>
```

---

## Tenants

### Create tenant
`POST /api/v1/tenants`

No authentication required. Returns an API key — store it securely.

**Request:**
```json
{ "name": "my-org" }
```

**Response `201`:**
```json
{
  "id": "eca07f03-8807-41fa-ac7d-53ea2ca5ec16",
  "name": "my-org",
  "apiKey": "8dc2a23a-74bd-4c13-8866-7366208c4e47",
  "createdAt": "2026-03-23T10:00:00Z"
}
```

---

## Agents

### List agents
`GET /api/v1/agents`

**Response `200`:**
```json
[
  {
    "id": "1ba46957-71ad-4a5b-b45c-fd9b023c794d",
    "tenantId": "eca07f03-...",
    "name": "OpenClaw Assistant",
    "description": "General-purpose assistant for developer workflows",
    "createdAt": "2026-03-23T10:01:00Z"
  }
]
```

### Create agent
`POST /api/v1/agents`

**Request:**
```json
{
  "tenantId": "eca07f03-8807-41fa-ac7d-53ea2ca5ec16",
  "name": "Code Review Bot",
  "description": "Automated code review and quality analysis"
}
```

**Response `201`:** Agent object (same shape as list item)

### Get agent metrics
`GET /api/v1/agents/{id}/metrics`

**Response `200`:**
```json
{
  "totalTraces": 142,
  "avgTokens": 3847.5,
  "avgCost": 0.023100,
  "successRate": 94.4,
  "p95LatencyMs": 4820
}
```

---

## Traces

### Create trace (with optional spans)
`POST /api/v1/traces`

**Request:**
```json
{
  "agentId": "1ba46957-71ad-4a5b-b45c-fd9b023c794d",
  "status": "COMPLETED",
  "totalTokens": 2340,
  "totalCost": 0.031200,
  "metadata": { "model": "claude-sonnet-4-6", "version": "1.0" },
  "spans": [
    {
      "name": "web_search",
      "type": "TOOL_CALL",
      "status": "COMPLETED",
      "input": "{\"query\": \"Spring Boot best practices\"}",
      "output": "{\"results\": [...]}",
      "sortOrder": 0
    },
    {
      "name": "claude-sonnet-completion",
      "type": "LLM_CALL",
      "status": "COMPLETED",
      "model": "claude-sonnet-4-6",
      "inputTokens": 780,
      "outputTokens": 1560,
      "cost": 0.025740,
      "input": "You are a helpful assistant. Context: ...",
      "output": "Here is my analysis...",
      "sortOrder": 1
    }
  ]
}
```

**`status` values:** `RUNNING` | `COMPLETED` | `FAILED`  
**`type` values:** `LLM_CALL` | `TOOL_CALL` | `RETRIEVAL` | `CUSTOM`

**Response `201`:** Trace object with all spans included.

### List traces (paginated)
`GET /api/v1/traces?agentId={id}&page=0&size=20&sort=startedAt,desc`

**Response `200`:**
```json
{
  "content": [ /* Trace objects */ ],
  "totalElements": 142,
  "totalPages": 8,
  "size": 20,
  "number": 0
}
```

### Get trace with spans
`GET /api/v1/traces/{id}`

**Response `200`:** Full Trace object with `spans` array ordered by `sortOrder`.

**Response `404`:** Trace not found.

---

## Real-time streaming

### Subscribe to live span events
`GET /api/v1/traces/{id}/stream?apiKey={key}`

Returns an `text/event-stream` (SSE) connection. Events are emitted as each
span is added to the trace.

**Event format:**
```
event: span
data: { /* full Trace object with all current spans */ }
```

**Browser usage:**
```javascript
const es = new EventSource(
  `/api/v1/traces/${traceId}/stream?apiKey=${apiKey}`
)
es.addEventListener('span', (e) => {
  const trace = JSON.parse(e.data)
  updateCanvas(trace.spans)
})
```

Note: `EventSource` does not support custom headers — pass `apiKey` as a query
parameter for SSE endpoints only.

---

## Error responses

All errors follow [RFC 7807 Problem Detail](https://datatracker.ietf.org/doc/html/rfc7807):

```json
{
  "timestamp": "2026-03-23T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/v1/traces"
}
```

| Status | Meaning |
|--------|---------|
| `400` | Validation error — check request body |
| `401` | Missing `X-API-Key` header |
| `403` | Invalid API key |
| `404` | Resource not found |
| `500` | Unexpected server error — check logs |

---

## SDK quick-start (curl)

```bash
# 1. Create tenant
TENANT=$(curl -s -X POST http://localhost:8080/api/v1/tenants \
  -H "Content-Type: application/json" \
  -d '{"name":"my-org"}')
API_KEY=$(echo $TENANT | jq -r .apiKey)

# 2. Create agent
AGENT=$(curl -s -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d "{\"tenantId\":\"$(echo $TENANT | jq -r .id)\",\"name\":\"My Agent\"}")
AGENT_ID=$(echo $AGENT | jq -r .id)

# 3. Submit a completed trace
curl -s -X POST http://localhost:8080/api/v1/traces \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d "{
    \"agentId\": \"$AGENT_ID\",
    \"status\": \"COMPLETED\",
    \"totalTokens\": 1200,
    \"totalCost\": 0.018000,
    \"spans\": [
      {\"name\":\"llm-call\",\"type\":\"LLM_CALL\",\"status\":\"COMPLETED\",
       \"model\":\"claude-sonnet-4-6\",\"inputTokens\":400,\"outputTokens\":800,
       \"cost\":0.018000,\"sortOrder\":0}
    ]
  }"
```
