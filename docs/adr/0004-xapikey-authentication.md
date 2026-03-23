# ADR-0004: X-API-Key header authentication

**Status:** Accepted  
**Date:** 2026-03-23  
**Deciders:** Adriano Viera dos Santos

---

## Context

HookWatch needs a simple, programmatic authentication mechanism for its REST API.
The primary consumers are backend services and SDK clients, not browsers with
session cookies. The following options were evaluated:

| Option | Use case fit | Complexity | Revocability |
|--------|-------------|------------|--------------|
| X-API-Key (opaque token) | ✅ Machine-to-machine | Low | Per-key revocation |
| JWT (Bearer token) | Browser + M2M | Medium | Short-lived; no server-side revocation without blocklist |
| OAuth 2.0 Client Credentials | Enterprise M2M | High | Full | 
| HTTP Basic | Legacy | Low | ⚠️ Password in every request |

---

## Decision

Use **`X-API-Key` header** with opaque UUIDs stored in the `tenants` table.

Implementation: `ApiKeyFilter` (`OncePerRequestFilter`) validates the header
against `TenantRepository.findByApiKey()` on every request.

Bypass list (no auth required):
- `POST /api/v1/tenants` — bootstrap: creates a new tenant and returns the API key
- `/swagger-ui/**`, `/v3/api-docs/**` — developer tooling
- SSE endpoints also accept `?apiKey=` query parameter (browsers cannot set
  headers on `EventSource`)

---

## Known limitations (future work)

1. **Plain text storage** — API keys are stored unhashed. Should migrate to
   `bcrypt` hash + timing-safe comparison before production hardening.
   See `REVIEW.md`.

2. **No rate limiting** — `POST /api/v1/tenants` is publicly accessible and
   could be abused to create many tenants. A `RateLimiter` (Bucket4j or
   nginx `limit_req`) should be added.

3. **API key in SSE URL** — appears in nginx/application access logs. Mitigated
   long-term by a short-lived token exchange: client calls
   `POST /api/v1/stream-tokens` with API key → receives a one-time token valid
   for 60s for the SSE connection.

---

## Consequences

- **Positive:** Dead simple. One HTTP header, one DB lookup, no token refresh.
- **Positive:** Per-tenant revocation: rotate `apiKey` column value, instant effect.
- **Negative:** Key rotation requires client-side update. No built-in expiry.
  Acceptable for the current use case; JWT can replace this if short-lived
  sessions become a requirement.
