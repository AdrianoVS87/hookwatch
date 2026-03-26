package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TraceComparisonIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("compare-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        String tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "compare-agent");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCompareTracesAndReturnDelta() {
        UUID traceId1 = createTraceWithSpans(apiKey, agentId, 1000, "0.0030", 2);
        UUID traceId2 = createTraceWithSpans(apiKey, agentId, 1500, "0.0045", 3);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/compare?traceId1=" + traceId1 + "&traceId2=" + traceId2,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("trace1");
        assertThat(body).containsKey("trace2");
        assertThat(body).containsKey("delta");

        Map delta = (Map) body.get("delta");
        assertThat(delta).isNotNull();
        assertThat((Integer) delta.get("tokensDiff")).isEqualTo(500);
        assertThat((Boolean) delta.get("statusMatch")).isTrue();
        assertThat((Integer) delta.get("spanCountDiff")).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroDeltaWhenComparingSameTrace() {
        UUID traceId = createTraceWithSpans(apiKey, agentId, 1000, "0.0030", 2);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/compare?traceId1=" + traceId + "&traceId2=" + traceId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> delta = (Map<String, Object>) body.get("delta");
        assertThat((Integer) delta.get("tokensDiff")).isEqualTo(0);
        assertThat((Integer) delta.get("spanCountDiff")).isEqualTo(0);
        assertThat((Boolean) delta.get("statusMatch")).isTrue();
    }

    @Test
    void shouldReturn403WhenComparingTraceFromDifferentTenant() {
        UUID traceId1 = createTraceWithSpans(apiKey, agentId, 1000, "0.0030", 1);

        // Create a second tenant with its own trace
        String[] otherKeyAndId = createTenantAndGetKeyAndId("other-tenant-" + UUID.randomUUID());
        String otherApiKey = otherKeyAndId[0];
        String otherTenantId = otherKeyAndId[1];
        UUID otherAgent = createAgent(otherApiKey, otherTenantId, "other-agent");
        UUID traceId2 = createTraceWithSpans(otherApiKey, otherAgent, 500, "0.0010", 1);

        // Try to compare using first tenant's API key — traceId2 belongs to other tenant
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/compare?traceId1=" + traceId1 + "&traceId2=" + traceId2,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturn404WhenTraceDoesNotExist() {
        UUID traceId1 = createTraceWithSpans(apiKey, agentId, 1000, "0.0030", 1);
        UUID fakeId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/compare?traceId1=" + traceId1 + "&traceId2=" + fakeId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID createTraceWithSpans(String key, UUID agent, int tokens, String cost, int spanCount) {
        StringBuilder spans = new StringBuilder("[");
        for (int i = 0; i < spanCount; i++) {
            if (i > 0) spans.append(",");
            spans.append("""
                {
                    "name": "span-%d",
                    "type": "LLM_CALL",
                    "status": "COMPLETED",
                    "inputTokens": %d,
                    "outputTokens": %d,
                    "sortOrder": %d
                }
                """.formatted(i, tokens / spanCount / 2, tokens / spanCount / 2, i));
        }
        spans.append("]");

        String traceJson = """
            {
                "agentId": "%s",
                "status": "COMPLETED",
                "totalTokens": %d,
                "totalCost": %s,
                "spans": %s
            }
            """.formatted(agent, tokens, cost, spans);

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(key));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces", entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }
}
