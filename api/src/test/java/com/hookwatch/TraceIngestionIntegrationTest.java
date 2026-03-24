package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIngestionIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("trace-ingestion-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        String tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "test-agent");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateTraceWith3SpansAndVerifyPersistence() {
        String traceJson = """
            {
                "agentId": "%s",
                "status": "COMPLETED",
                "totalTokens": 1500,
                "totalCost": 0.0045,
                "spans": [
                    {
                        "name": "root-span",
                        "type": "LLM_CALL",
                        "status": "COMPLETED",
                        "inputTokens": 500,
                        "outputTokens": 300,
                        "cost": 0.0020,
                        "model": "gpt-4",
                        "sortOrder": 0
                    },
                    {
                        "name": "tool-call-span",
                        "type": "TOOL_CALL",
                        "status": "COMPLETED",
                        "inputTokens": 200,
                        "outputTokens": 100,
                        "cost": 0.0010,
                        "sortOrder": 1
                    },
                    {
                        "name": "child-llm-span",
                        "type": "LLM_CALL",
                        "status": "COMPLETED",
                        "inputTokens": 300,
                        "outputTokens": 100,
                        "cost": 0.0015,
                        "model": "gpt-4",
                        "sortOrder": 2
                    }
                ]
            }
            """.formatted(agentId);

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                baseUrl() + "/traces", entity, Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String traceId = (String) createResponse.getBody().get("id");
        assertThat(traceId).isNotNull();

        // GET the trace back
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = getResponse.getBody();
        assertThat(body.get("totalTokens")).isEqualTo(1500);

        List<Map<String, Object>> spans = (List<Map<String, Object>>) body.get("spans");
        assertThat(spans).hasSize(3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldVerifyParentChildSpanRelationships() {
        UUID parentSpanId = UUID.randomUUID();
        String traceJson = """
            {
                "agentId": "%s",
                "status": "COMPLETED",
                "totalTokens": 800,
                "totalCost": 0.002,
                "spans": [
                    {
                        "name": "parent-span",
                        "type": "LLM_CALL",
                        "status": "COMPLETED",
                        "sortOrder": 0
                    },
                    {
                        "name": "child-span",
                        "type": "TOOL_CALL",
                        "status": "COMPLETED",
                        "parentSpanId": "%s",
                        "sortOrder": 1
                    }
                ]
            }
            """.formatted(agentId, parentSpanId);

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String traceId = (String) response.getBody().get("id");
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        List<Map<String, Object>> spans = (List<Map<String, Object>>) getResponse.getBody().get("spans");
        Map<String, Object> childSpan = spans.stream()
                .filter(s -> "child-span".equals(s.get("name")))
                .findFirst().orElseThrow();
        assertThat(childSpan.get("parentSpanId")).isEqualTo(parentSpanId.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateTraceWithEmptySpansArray() {
        String traceJson = """
            {
                "agentId": "%s",
                "status": "COMPLETED",
                "totalTokens": 0,
                "totalCost": 0.0,
                "spans": []
            }
            """.formatted(agentId);

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<Map<String, Object>> spans = (List<Map<String, Object>>) response.getBody().get("spans");
        assertThat(spans).isEmpty();
    }

    @Test
    void shouldReturn400ForMissingRequiredFields() {
        // Missing agentId and status
        String traceJson = """
            {
                "totalTokens": 100
            }
            """;

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
