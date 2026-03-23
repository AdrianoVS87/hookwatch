/**
 * Request filters for the HookWatch API.
 *
 * <h2>Authentication Policy</h2>
 * <p>All API requests (except /swagger-ui, /actuator, POST /api/v1/tenants) must
 * include a valid API key in the {@code X-API-Key} HTTP header.</p>
 *
 * <p><strong>Query parameter authentication (?apiKey=) is NOT supported.</strong>
 * This prevents API key leakage in server access logs, proxy logs, and browser history.
 * SSE endpoints (future) and all other endpoints use header-only authentication.</p>
 *
 * <p>Web clients that need SSE should use {@code fetch()} with {@code ReadableStream}
 * instead of {@code EventSource} (which doesn't support custom headers).</p>
 *
 * @see com.hookwatch.filter.ApiKeyFilter
 */
package com.hookwatch.filter;
