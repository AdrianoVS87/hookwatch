package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TraceComparisonIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private String tenantId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("compare-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "compare-agent");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnComparisonWithDeltas() {
        UUID traceId1 = createTraceWithSpans(apiKey, agentId, "COMPLETED", 500, "0.005", 2);
        UUID traceId2 = createTraceWithSpans(apiKey, agentId, "COMPLETED", 800, "0.012", 3);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/compare?traceId1=" + traceId1 + "&traceId2=" + traceId2,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body).isNotNull();

        // Verify traces are present
        assertThat(body.get("trace1")).isNotNull();
        assertThat(body.get("trace2")).isNotNull();

        // Verify delta
        Map<String, Object> delta = (Map<String, Object>) body.get("delta");
        assertThat(delta).isNotNull();
        assertThat(((Number) delta.get("tokensDiff")).intValue()).isEqualTo(300); // 800 - 500
        assertThat((Boolean) delta.get("statusMatch")).isTrue();
        assertThat(((Number) delta.get("spanCountDiff")).intValue()).isEqualTo(1); // 3 - 2
    }

    @Test
    void shouldReturn404ForNonExistentTrace() {
        UUID traceId1 = createTrace(apiKey, agentId, "COMPLETED", 500, "0.005");
        UUID fakeId = UUID.randomUUID();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/compare?traceId1=" + traceId1 + "&traceId2=" + fakeId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn403ForOtherTenantsTrace() {
        UUID traceId1 = createTrace(apiKey, agentId, "COMPLETED", 500, "0.005");

        // Create a second tenant with its own trace
        String[] otherKeyAndId = createTenantAndGetKeyAndId("other-tenant-" + UUID.randomUUID());
        String otherApiKey = otherKeyAndId[0];
        String otherTenantId = otherKeyAndId[1];
        UUID otherAgentId = createAgent(otherApiKey, otherTenantId, "other-agent");
        UUID otherTraceId = createTrace(otherApiKey, otherAgentId, "COMPLETED", 300, "0.003");

        // Try to compare traces from different tenants — should get 403
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/compare?traceId1=" + traceId1 + "&traceId2=" + otherTraceId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Creates a trace with the specified number of spans.
     */
    @SuppressWarnings("unchecked")
    private UUID createTraceWithSpans(String apiKey, UUID agentId, String status, int tokens, String cost, int spanCount) {
        StringBuilder spansJson = new StringBuilder("[");
        for (int i = 0; i < spanCount; i++) {
            if (i > 0) spansJson.append(",");
            spansJson.append("""
                {
                    "name": "span-%d",
                    "type": "LLM_CALL",
                    "status": "COMPLETED",
                    "inputTokens": %d,
                    "outputTokens": %d,
                    "sortOrder": %d
                }
                """.formatted(i, 100 + i * 50, 200 + i * 50, i));
        }
        spansJson.append("]");

        String traceJson = """
            {
                "agentId": "%s",
                "status": "%s",
                "totalTokens": %d,
                "totalCost": %s,
                "spans": %s
            }
            """.formatted(agentId, status, tokens, cost, spansJson);

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces", entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }
}
