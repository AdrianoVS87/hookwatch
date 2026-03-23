# ADR-0002: Server-Sent Events (SSE) over WebSocket for live trace updates

**Status:** Accepted  
**Date:** 2026-03-23  
**Deciders:** Adriano Viera dos Santos

---

## Context

The React Flow canvas in the frontend needs to receive live span updates as
traces execute — new nodes should appear on the graph without a page refresh.
Two primary options exist for server-push in HTTP/1.1+:

| Option | Protocol | Direction | Complexity | Load balancer support |
|--------|----------|-----------|------------|----------------------|
| Server-Sent Events (SSE) | HTTP/1.1 | Server → Client only | Low | ✅ Works out of the box |
| WebSocket | WS/WSS | Bidirectional | Higher | ⚠️ Requires sticky sessions or pub/sub broker |
| Long polling | HTTP | Simulated server push | Medium | ✅ Works out of the box |

---

## Decision

Use **Spring MVC `SseEmitter`** for the `/api/v1/traces/{id}/stream` endpoint.

Reasoning:

1. **Unidirectional is sufficient** — the canvas only needs to *receive* updates
   from the server. There is no client-to-server stream required for trace display.
   WebSocket's bidirectionality adds complexity without benefit here.

2. **No broker required** — SSE works over plain HTTP. WebSockets need sticky
   sessions behind a load balancer or a pub/sub broker (Redis Pub/Sub, Kafka).
   For the current scale, SSE over stateless HTTP is correct.

3. **Auto-reconnect in browsers** — the `EventSource` API reconnects automatically
   on disconnect with exponential backoff. This is built into all modern browsers.

4. **`SseEmitter` is idiomatic Spring MVC** — integrates naturally with the
   existing `@RestController` layer. No reactive stack required.

5. **HTTP/2 multiplexing** — if the app is fronted by nginx with HTTP/2, SSE
   connections are multiplexed over a single TCP connection, eliminating the
   browser's 6-connection-per-domain limit from HTTP/1.1.

---

## Trade-offs accepted

- SSE does not support binary frames — all events are UTF-8 text. Span payloads
  are JSON, so this is not a limitation.
- SSE state lives in-process (`ConcurrentHashMap` in `TraceEventPublisher`). If
  the API scales to multiple pods, emitters must migrate to Redis Pub/Sub. This
  is captured in `REVIEW.md` as a known future work item.
- `EventSource` does not natively support custom headers, so the API key is
  passed as a query parameter for SSE endpoints only. This is noted as a
  security trade-off (appears in access logs) pending a short-lived token
  exchange implementation.

---

## Consequences

- **Positive:** Zero additional infrastructure. No Kafka, no Redis Pub/Sub, no
  WebSocket handshake upgrade logic.
- **Positive:** Browser auto-reconnect = resilient to network interruptions.
- **Negative:** Horizontal scaling requires pub/sub broker. Acceptable until
  multi-pod deployment is needed.
